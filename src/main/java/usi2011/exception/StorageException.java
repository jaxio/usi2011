package usi2011.exception;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

public class StorageException extends UsiException {
    private static final long serialVersionUID = 1L;

    public StorageException(String message, Exception e) {
        super(message, INTERNAL_SERVER_ERROR, e);
    }
}