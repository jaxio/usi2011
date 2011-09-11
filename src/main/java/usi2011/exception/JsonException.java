package usi2011.exception;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

public class JsonException extends UsiException {
    private static final long serialVersionUID = 1L;

    public JsonException() {
        super("", BAD_REQUEST);
    }

    public JsonException(Exception e) {
        super(e.getMessage(), BAD_REQUEST);
    }

    public JsonException(String message) {
        super(message, BAD_REQUEST);
    }
}