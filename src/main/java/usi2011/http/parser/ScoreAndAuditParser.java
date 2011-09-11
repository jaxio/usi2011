package usi2011.http.parser;

import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;
import static usi2011.util.FastEmailValidator.isEmail;
import static usi2011.util.Specifications.AUTHENTICATION_KEY;
import static usi2011.util.Specifications.AUTHENTICATION_SECRET_KEY;
import static usi2011.util.Specifications.SCORE_USER_EMAIL;

import java.util.List;
import java.util.Map;

import org.jboss.netty.handler.codec.http.QueryStringDecoder;

import usi2011.exception.AuthenticationKeyException;
import usi2011.exception.UserToAuditException;

public final class ScoreAndAuditParser {
    private final String email;

    public ScoreAndAuditParser(final String uri) {
        final Map<String, List<String>> decode = new QueryStringDecoder(uri).getParameters();
        assertAuthentication(decode);
        this.email = getEmail(decode);
    }

    private String getEmail(final Map<String, List<String>> decode) {
        final List<String> emails = decode.get(SCORE_USER_EMAIL);
        if (emails == null || emails.size() != 1) {
            throw new UserToAuditException("Missing email");
        }
        final String email = emails.get(0);
        if (!isEmail(email)) {
            throw new UserToAuditException("Invalid email");
        }
        return email;
    }

    private void assertAuthentication(final Map<String, List<String>> decode) {
        final List<String> authentications = decode.get(AUTHENTICATION_KEY);
        if (authentications == null || authentications.size() != 1) {
            throw new AuthenticationKeyException("Missing authentication");
        }
        if (!AUTHENTICATION_SECRET_KEY.equals(authentications.get(0))) {
            throw new AuthenticationKeyException("Wrong authentication key");
        }
    }

    public String getEmail() {
        return email;
    }
    
    @Override
    public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }
}