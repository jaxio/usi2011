package usi2011.exception;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

public class LoginException extends UsiException {
    private static final long serialVersionUID = 1L;

    public LoginException(String message) {
        super(message, BAD_REQUEST);
    }
}