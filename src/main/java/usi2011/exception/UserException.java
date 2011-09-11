package usi2011.exception;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

public class UserException extends UsiException {
    private static final long serialVersionUID = 1L;

    public UserException(String message) {
        super(message, BAD_REQUEST);
    }
}