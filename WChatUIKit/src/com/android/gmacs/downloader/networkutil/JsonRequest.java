package com.android.gmacs.downloader.networkutil;

import android.text.TextUtils;

import com.android.gmacs.downloader.AuthFailureError;
import com.android.gmacs.downloader.HttpHeaderParser;
import com.android.gmacs.downloader.NetworkResponse;
import com.android.gmacs.downloader.ParseError;
import com.android.gmacs.downloader.Request;
import com.android.gmacs.downloader.Response;
import com.android.gmacs.downloader.VolleyLog;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhouwei on 2017/7/12.
 * <p>
 * 请求返回String
 */
public class JsonRequest extends Request<String> {
    /**
     * Default charset for JSON request.
     */
    private static final String PROTOCOL_CHARSET = "utf-8";

    /**
     * Content type for request.
     */
    private static final String PROTOCOL_CONTENT_TYPE =
            String.format("application/x-www-form-urlencoded; charset=%s", PROTOCOL_CHARSET);

    private Map<String, String> headers = new HashMap<>();
    private Response.Listener<String> requestCallback;
    private String requestBody;

    /**
     * @param method          请求的方法 {@link Method}
     * @param url             url
     * @param params          请求的body eg:name=zhangsan&age=11
     * @param requestCallback 请求的回调
     */
    public JsonRequest(int method, String url, String params, RequestCallback requestCallback) {
        super(method, url, requestCallback);
        this.requestCallback = requestCallback;
        this.requestBody = params;
    }

    /**
     * @param method          请求的方法 {@link Method}
     * @param url             url
     * @param params          请求的参数 key=value
     * @param requestCallback 请求的回调
     */
    public JsonRequest(int method, String url, HashMap<String, String> params, RequestCallback requestCallback) {
        super(method, url, requestCallback);
        this.requestCallback = requestCallback;
        if (null != params && params.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> en : params.entrySet()) {
                sb.append(en.getKey()).append("=").append(en.getValue()).append("&");
            }
            requestBody = sb.toString();
        }
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
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return Response.error(new ParseError(e));
        }
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

    public Request<String> setHeaders(String key, String value) {
        headers.put(key, value);
        return this;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers;
    }
}
