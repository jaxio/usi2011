package usi2011.util;

import static java.lang.Math.max;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.fest.assertions.Assertions.assertThat;
import static usi2011.util.Specifications.AUTHENTICATION_KEY;
import static usi2011.util.Specifications.AUTHENTICATION_SECRET_KEY;
import static usi2011.util.Specifications.USER_INFO_MAX_LENGTH;
import static usi2011.util.Specifications.USER_INFO_MIN_LENGTH;

import org.junit.Test;

import usi2011.exception.AuthenticationKeyException;
import usi2011.exception.JsonException;

public class JSONBackedObjectTest {
    class JsonObject extends JSONBackedObject {
        @Override
        protected RuntimeException buildException(String message) {
            return new JsonException();
        }
    }

    @Test
    public void assertLength() {
        Json json = new Json().put("key", randomAlphabetic(USER_INFO_MIN_LENGTH + 1));
        new JsonObject().getAndAssertString(json, "key");
    }

    @Test(expected = JsonException.class)
    public void assertLengthThrowsExceptionWhenValueIsTooShow() {
        Json json = new Json().put("key", randomAlphabetic(USER_INFO_MIN_LENGTH - 1));
        new JsonObject().getAndAssertString(json, "key");
    }

    @Test(expected = JsonException.class)
    public void assertLengthThrowsExceptionWhenValueIsTooLong() {
        Json json = new Json().put("key", randomAlphabetic(USER_INFO_MAX_LENGTH + 1));
        new JsonObject().getAndAssertString(json, "key");
    }

    @Test(expected = JsonException.class)
    public void valueTooLongThrowsException() {
        new JsonObject().assertLength("key", randomAlphabetic(USER_INFO_MAX_LENGTH) + 1);
    }

    @Test(expected = JsonException.class)
    public void valueShortLongThrowsException() {
        new JsonObject().assertLength("key", randomAlphabetic(max(0, USER_INFO_MIN_LENGTH - 1)));
    }

    @Test
    public void getInt() {
        Json json = new Json().put("key", "1");
        assertThat(new JsonObject().getInt(json, "key")).isEqualTo(1);
    }

    @Test(expected = JsonException.class)
    public void missingIntThrowsException() {
        new JsonObject().getInt(new Json(), "key");
    }

    @Test(expected = JsonException.class)
    public void invalidIntThrowsException() {
        Json json = new Json().put("key", "not an int");
        new JsonObject().getInt(json, "key");
    }

    @Test
    public void getEmail() {
        Json json = new Json().put("key", "email@domain.com");
        assertThat(new JsonObject().getEmail(json, "key")).isEqualTo("email@domain.com");
    }

    @Test(expected = JsonException.class)
    public void missingEmailThrowsException() {
        new JsonObject().getEmail(new Json(), "key");
    }

    @Test(expected = JsonException.class)
    public void invalidEmailThrowsException() {
        Json json = new Json().put("key", "not an email");
        new JsonObject().getEmail(json, "key");
    }

    @Test
    public void assertAuthenticationKey() {
        Json json = new Json().put(AUTHENTICATION_KEY, AUTHENTICATION_SECRET_KEY);
        new JsonObject().assertAuthenticationKey(json);
    }

    @Test(expected = AuthenticationKeyException.class)
    public void missingAuthenticationThrowsException() {
        new JsonObject().assertAuthenticationKey(new Json());
    }

    @Test(expected = AuthenticationKeyException.class)
    public void invalidAuthenticationThrowsException() {
        Json json = new Json().put(AUTHENTICATION_KEY, "not a valid key");
        new JsonObject().assertAuthenticationKey(json);
    }
}