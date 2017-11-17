

package com.android.gmacs.downloader;

/**
 * Indicates that the server's response could not be parsed.
 */
@SuppressWarnings("serial")
public class ParseError extends VolleyError {
    public ParseError() {
    }

    public ParseError(NetworkResponse networkResponse) {
        super(networkResponse);
    }

    public ParseError(Throwable cause) {
        super(cause);
    }
}
