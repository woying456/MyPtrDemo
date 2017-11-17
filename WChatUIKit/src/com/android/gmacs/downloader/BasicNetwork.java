
package com.android.gmacs.downloader;

import android.text.TextUtils;

import com.common.gmacs.utils.CloseUtil;
import com.common.gmacs.utils.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A network performing Volley requests over an {@link HttpStack}.
 */
public class BasicNetwork implements Network {
    protected static final boolean DEBUG = VolleyLog.DEBUG;

    private static int DEFAULT_POOL_SIZE = 4096;

    protected final HttpStack mHttpStack;

    protected final ByteArrayPool mPool;

    /**
     * @param httpStack HTTP stack to be used
     */
    public BasicNetwork(HttpStack httpStack) {
        // If a pool isn't passed in, then build a small default pool that will give us a lot of
        // benefit and not use too much memory.
        this(httpStack, new ByteArrayPool(DEFAULT_POOL_SIZE));
    }

    /**
     * @param httpStack HTTP stack to be used
     * @param pool      a buffer pool that improves GC performance in copy operations
     */
    public BasicNetwork(HttpStack httpStack, ByteArrayPool pool) {
        mHttpStack = httpStack;
        mPool = pool;
    }

    /**
     * Attempts to prepare the request for a retry. If there are no more attempts remaining in the
     * request's retry policy, a timeout exception is thrown.
     *
     * @param request The request to use.
     */
    private static void attemptRetryOnException(String logPrefix, Request<?> request,
                                                VolleyError exception) throws VolleyError {
        RetryPolicy retryPolicy = request.getRetryPolicy();
        int oldTimeout = request.getTimeoutMs();

        try {
            retryPolicy.retry(exception);
        } catch (VolleyError e) {
            request.addMarker(
                    String.format("%s-timeout-giveup [timeout=%s]", logPrefix, oldTimeout));
            throw e;
        }
        request.addMarker(String.format("%s-retry [timeout=%s]", logPrefix, oldTimeout));
    }

    @Override
    public NetworkResponse performRequest(Request<?> request) throws VolleyError {
        while (true) {
            URLHttpResponse httpResponse = null;
            byte[] responseContents = null;
            Map<String, String> responseHeaders = Collections.emptyMap();
            try {
                // Gather headers.
                Map<String, String> headers = new HashMap<>();
                addCacheHeaders(headers, request.getCacheEntry());
                httpResponse = mHttpStack.performRequest(request, headers);

                int statusCode = httpResponse.getResponseCode();

                responseHeaders = httpResponse.getHeaders();
                // Handle cache validation.
                if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED) {

                    Cache.Entry entry = request.getCacheEntry();
                    if (entry == null) {
                        return new NetworkResponse(HttpURLConnection.HTTP_NOT_MODIFIED, null,
                                responseHeaders, true);
                    }

                    // A HTTP 304 response does not have all header fields. We
                    // have to use the header fields from the cache entry plus
                    // the new ones from the response.
                    // http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5
                    entry.responseHeaders.putAll(responseHeaders);
                    return new NetworkResponse(HttpURLConnection.HTTP_NOT_MODIFIED, entry.data,
                            entry.responseHeaders, true);
                }

                // Some responses such as 204s do not have content.  We must check.
                if (httpResponse.getContentStream() != null) {
                    if (TextUtils.isEmpty(request.getDownloadPath())) {
                        responseContents = entityToBytes(httpResponse);
                    } else {
                        responseContents = entityToFile(httpResponse, request.getDownloadPath());
                    }
                } else {
                    // Add 0 byte response as a way of honestly representing a
                    // no-content request.
                    responseContents = new byte[0];
                }

                if (statusCode < 200 || statusCode > 299) {
                    throw new IOException();
                }
                return new NetworkResponse(statusCode, responseContents, responseHeaders, false);
            } catch (SocketTimeoutException e) {
                attemptRetryOnException("socket", request, new TimeoutError());
            } catch (MalformedURLException e) {
                throw new RuntimeException("Bad URL " + request.getUrl(), e);
            } catch (IOException e) {
                int statusCode;
                NetworkResponse networkResponse = null;
                if (httpResponse != null) {
                    statusCode = httpResponse.getResponseCode();
                } else {
                    throw new NoConnectionError(e);
                }
                VolleyLog.e("Unexpected response code %d for %s", statusCode, request.getUrl());
                if (responseContents != null) {
                    networkResponse = new NetworkResponse(statusCode, responseContents,
                            responseHeaders, false);
                    if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED ||
                            statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
                        attemptRetryOnException("auth",
                                request, new AuthFailureError(networkResponse));
                    } else {
                        // TODO: Only throw ServerError for 5xx status codes.
                        throw new ServerError(networkResponse);
                    }
                } else {
                    throw new NetworkError();
                }
            }
        }
    }

    private void addCacheHeaders(Map<String, String> headers, Cache.Entry entry) {
        // If there's no cache entry, we're done.
        if (entry == null) {
            return;
        }

        if (entry.etag != null) {
            headers.put("If-None-Match", entry.etag);
        }

        if (entry.lastModified > 0) {
            Date refTime = new Date(entry.lastModified);
            DateFormat sdf = SimpleDateFormat.getDateTimeInstance();
            headers.put("If-Modified-Since", sdf.format(refTime));
        }
    }

    /**
     * Reads the contents of HttpEntity into a byte[].
     */
    private byte[] entityToBytes(URLHttpResponse httpResponse) throws IOException, ServerError {
        PoolingByteArrayOutputStream bytes =
                new PoolingByteArrayOutputStream(mPool, (int) httpResponse.getContentLength());
        byte[] buffer = null;
        try {
            InputStream in = httpResponse.getContentStream();
            if (in == null) {
                throw new ServerError();
            }
            buffer = mPool.getBuf(1024);
            int count;
            while ((count = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
            return bytes.toByteArray();
        } finally {
            CloseUtil.closeQuietly(httpResponse.getContentStream());
            mPool.returnBuf(buffer);
            CloseUtil.closeQuietly(bytes);
        }
    }

    private byte[] entityToFile(URLHttpResponse httpResponse, String downloadPath) throws IOException {
        File file = new File(downloadPath);
        file.mkdirs();
        File outputFile = new File(file, FileUtil.getFileName());
        FileOutputStream out = null;
        InputStream in = httpResponse.getContentStream();
        try {
            out = new FileOutputStream(outputFile);
            byte[] buf = new byte[4096];
            while (true) {
                int read = in.read(buf);
                if (read == -1) {
                    break;
                }
                out.write(buf, 0, read);
            }
            out.flush();
        } finally {
            CloseUtil.closeQuietly(out);
        }
        return outputFile.getAbsolutePath().getBytes();
    }
}
