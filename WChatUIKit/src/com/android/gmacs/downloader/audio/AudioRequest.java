package com.android.gmacs.downloader.audio;

import com.android.gmacs.downloader.HttpHeaderParser;
import com.android.gmacs.downloader.NetworkResponse;
import com.android.gmacs.downloader.Request;
import com.android.gmacs.downloader.Response;
import com.android.gmacs.downloader.VolleyError;
import com.common.gmacs.utils.FileUtil;

public class AudioRequest extends Request<String> {

    private static final String sDownloadPath = FileUtil.getCacheDir("audio").getAbsolutePath();
    private Response.Listener<String> mListener;

    public AudioRequest(String url, Response.ErrorListener errorListener, Response.Listener<String> listener) {
        super(Method.GET, url, errorListener);
        this.mListener = listener;
        setDownloadPath(sDownloadPath);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        if (response.data == null) {
            return Response.error(new VolleyError(response));
        } else {
            return Response.success(new String(response.data), HttpHeaderParser.parseCacheHeaders(response));
        }
    }

    @Override
    protected Response<String> parseLocalResponse(String fileName) {
        return null;
    }

    @Override
    protected void deliverResponse(String response) {
        if (mListener != null) {
            mListener.onResponse(response);
        }
    }
}
