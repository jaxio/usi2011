package usi2011.http.parser;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.util.Specifications.AUTHENTICATION_KEY;
import static usi2011.util.Specifications.AUTHENTICATION_SECRET_KEY;
import static usi2011.util.Specifications.SCORE_USER_EMAIL;

import org.junit.Test;

import usi2011.exception.AuthenticationKeyException;
import usi2011.exception.UserToAuditException;

public class ScoreAndAuditParserTest {
    @Test
    public void valid() {
        ScoreAndAuditParser scoreAndAuditParser = new ScoreAndAuditParser("score" //
                + "?" + SCORE_USER_EMAIL + "=email@domain.com" //
                + "&" + AUTHENTICATION_KEY + "=" + AUTHENTICATION_SECRET_KEY);
        assertThat(scoreAndAuditParser.getEmail()).isEqualTo("email@domain.com");
    }

    @Test(expected = UserToAuditException.class)
    public void missingEmailThrowsException() {
        new ScoreAndAuditParser("score?" + AUTHENTICATION_KEY + "=" + AUTHENTICATION_SECRET_KEY);
    }

    @Test(expected = UserToAuditException.class)
    public void invalidEmailValueThrowsException() {
        new ScoreAndAuditParser("score?" + SCORE_USER_EMAIL + "=invalid email&" + AUTHENTICATION_KEY + "=" + AUTHENTICATION_SECRET_KEY);
    }

    @Test(expected = AuthenticationKeyException.class)
    public void missingAuthenticationThrowsException() {
        new ScoreAndAuditParser("score?" + SCORE_USER_EMAIL + "=email@domain.com");
    }

    @Test(expected = AuthenticationKeyException.class)
    public void invalidAuthenticationKeyThrowsException() {
        new ScoreAndAuditParser("score?" + SCORE_USER_EMAIL + "=email@domain.com?" + AUTHENTICATION_KEY + "=invalid key");
    }
}