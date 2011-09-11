package usi2011.exception;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

public class GameSessionException extends UsiException {
    private static final long serialVersionUID = 1L;

    public GameSessionException(String message) {
        super(message, BAD_REQUEST);
    }

    public GameSessionException(String message, Exception e) {
        super(message, BAD_REQUEST, e);
    }
}