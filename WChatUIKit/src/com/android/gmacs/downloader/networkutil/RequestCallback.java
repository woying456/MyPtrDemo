package com.android.gmacs.downloader.networkutil;

import com.android.gmacs.downloader.Response;
import com.android.gmacs.downloader.VolleyError;

public abstract class RequestCallback implements Response.Listener<String>, Response.ErrorListener {
    /**
     * 请求成功的回调
     */
    public abstract void onSuccessCallback(String result);

    /**
     * 请求失败的回调
     */
    public void onErrorCallback(String errorMessage) {
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        String errorMessage = error.getMessage();
        onErrorCallback(errorMessage);
    }

    @Override
    public void onResponse(String response) {
        onSuccessCallback(response);
    }
}
