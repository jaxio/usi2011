package usi2011.exception;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

/**
 * Base USI exception that support the associated {@link #httpResponseStatus} to show the user
 */
public abstract class UsiException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final HttpResponseStatus httpResponseStatus;

    public UsiException(String message, HttpResponseStatus httpResponseStatus) {
        super(message);
        this.httpResponseStatus = httpResponseStatus;
    }

    public UsiException(String message, HttpResponseStatus httpResponseStatus, Exception e) {
        super(message, e);
        this.httpResponseStatus = httpResponseStatus;
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return httpResponseStatus;
    }
}