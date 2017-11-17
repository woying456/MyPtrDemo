
package com.android.gmacs.downloader;


public interface ResponseDelivery {
    /**
     * Parses a response from the network or cache and delivers it.
     */
    void postResponse(Request<?> request, Response<?> response);

    /**
     * Parses a response from the network or cache and delivers it. The provided
     * Runnable will be executed after delivery.
     */
    void postResponse(Request<?> request, Response<?> response, Runnable runnable);

    /**
     * Posts an error for the given request.
     */
    void postError(Request<?> request, VolleyError error);

    void stop();
}
