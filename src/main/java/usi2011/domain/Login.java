package usi2011.domain;

import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;
import static usi2011.util.Specifications.USER_PASSWORD;
import usi2011.exception.LoginException;
import usi2011.util.JSONBackedObject;

public final class Login extends JSONBackedObject {
    private final String email;
    private final String password;

    public Login(final String email, final String password) {
        this.email = assertEmail(email);
        this.password = assertLength(USER_PASSWORD, password);
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public RuntimeException buildException(String message) {
        return new LoginException(message);
    }

    @Override
    public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }
}