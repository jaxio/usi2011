package usi2011.exception;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

public class NoGameCreatedException extends UsiException {
    private static final long serialVersionUID = 1L;

    public NoGameCreatedException(String message) {
        super(message, UNAUTHORIZED);
    }

    public NoGameCreatedException(String message, Exception e) {
        super(message, UNAUTHORIZED, e);
    }
}