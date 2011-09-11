package usi2011.exception;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

public class UserToAuditException extends UsiException {
    private static final long serialVersionUID = 1L;

    public UserToAuditException(String message) {
        super(message, UNAUTHORIZED);
    }
}