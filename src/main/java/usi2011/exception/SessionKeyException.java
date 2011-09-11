package usi2011.exception;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

public class SessionKeyException extends UsiException {
    private static final long serialVersionUID = 1L;

    public SessionKeyException(String message) {
        super(message, UNAUTHORIZED);
    }
}