package com.android.gmacs.downloader.networkutil;

import android.text.TextUtils;

import com.android.gmacs.downloader.AuthFailureError;
import com.android.gmacs.downloader.HttpHeaderParser;
import com.android.gmacs.downloader.NetworkResponse;
import com.android.gmacs.downloader.Request;
import com.android.gmacs.downloader.Response;
import com.android.gmacs.downloader.VolleyLog;
import com.common.gmacs.utils.GmacsEnvi;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class GZIPRequest extends Request<String> {
    /**
     * Default charset for JSON request.
     */
    private static final String PROTOCOL_CHARSET = "utf-8";

    /**
     * Content type for request.
     */
    private static final String PROTOCOL_CONTENT_TYPE =
            String.format("application/x-www-form-urlencoded; charset=%s", PROTOCOL_CHARSET);

    private Response.Listener<String> requestCallback;
    private String requestBody;

    /**
     * @param method          请求的方法 {@link Method}
     * @param url             url
     * @param params          请求的body eg:name=zhangsan&age=11
     * @param requestCallback 请求的回调
     */
    public GZIPRequest(int method, String url, String params, RequestCallback requestCallback) {
        super(method, url, requestCallback);
        this.requestCallback = requestCallback;
        this.requestBody = params;
        setDownloadPath(GmacsEnvi.appContext.getFilesDir().getAbsolutePath());
        setShouldCache(false);
    }

    @Override
    public String getBodyContentType() {
        return PROTOCOL_CONTENT_TYPE;
    }


    @Override
    protected void deliverResponse(String response) {
        if (requestCallback != null) {
            requestCallback.onResponse(response);
        }
    }

    @Override
    public Response<String> parseNetworkResponse(NetworkResponse response) {
        return Response.success(new String(response.data), HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected Response<String> parseLocalResponse(String fileName) {
        return null;
    }

    @Override
    public byte[] getBody() {
        try {
            return TextUtils.isEmpty(requestBody) ? null : requestBody.getBytes(PROTOCOL_CHARSET);
        } catch (UnsupportedEncodingException uee) {
            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s",
                    requestBody, PROTOCOL_CHARSET);
            return null;
        }
    }

    @Override
    protected String getParamsEncoding() {
        return PROTOCOL_CHARSET;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept-Encoding", "gzip, deflate");
        return headers;
    }
}
