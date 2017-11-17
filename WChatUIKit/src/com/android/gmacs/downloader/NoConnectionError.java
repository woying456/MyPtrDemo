

package com.android.gmacs.downloader;

/**
 * Error indicating that no connection could be established when performing a Volley request.
 */
@SuppressWarnings("serial")
public class NoConnectionError extends NetworkError {
    public NoConnectionError() {
        super();
    }

    public NoConnectionError(Throwable reason) {
        super(reason);
    }
}
