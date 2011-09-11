package usi2011.http.parser;

import static java.lang.Math.max;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.fest.assertions.Assertions.assertThat;
import static usi2011.http.parser.LoginParser.isGoldenPath;
import static usi2011.util.Specifications.USER_EMAIL;
import static usi2011.util.Specifications.USER_INFO_MAX_LENGTH;
import static usi2011.util.Specifications.USER_INFO_MIN_LENGTH;
import static usi2011.util.Specifications.USER_PASSWORD;

import org.junit.Ignore;
import org.junit.Test;

import usi2011.exception.JsonException;
import usi2011.exception.LoginException;
import usi2011.util.Json;

@Ignore
public class LoginParserTest {
    @Test
    public void jsonObject() {
        LoginParser loginParser = new LoginParser(new Json() //
                .put(USER_EMAIL, "email@domain.com") //
                .put(USER_PASSWORD, "password"));

        assertThat(loginParser.getEmail()).isEqualTo("email@domain.com");
        assertThat(loginParser.getPassword()).isEqualTo("password");
    }

    @Test
    public void jsonObjectWithEscape() {
        LoginParser loginParser = new LoginParser(new Json() //
                .put(USER_EMAIL, "email@domain.com") //
                .put(USER_PASSWORD, "pa\\u20AC\\u20ACword"));

        assertThat(loginParser.getEmail()).isEqualTo("email@domain.com");
        assertThat(loginParser.getPassword()).isEqualTo("pa\\u20AC\\u20ACword");
    }

    @Test(expected = LoginException.class)
    public void invalidEmailThrowsException() {
        new LoginParser(new Json() //
                .put(USER_EMAIL, "invalid email") //
                .put(USER_PASSWORD, "password"));
    }

    @Test(expected = LoginException.class)
    public void passwordTooShortThrowsException() {
        new LoginParser(new Json() //
                .put(USER_EMAIL, "email@domain.com") //
                .put(USER_PASSWORD, randomAlphabetic(max(0, USER_INFO_MIN_LENGTH - 1))));
    }

    @Test(expected = LoginException.class)
    public void passwordTooLongThrowsException() {
        new LoginParser(new Json() //
                .put(USER_EMAIL, "email@domain.com") //
                .put(USER_PASSWORD, randomAlphabetic(USER_INFO_MAX_LENGTH + 1)));
    }

    @Test
    public void goldenPath() {
        assertThat(isGoldenPath("{\"mail\":\"email@domain.com\",\"password\":\"password\"}".split("\""))).isTrue();
        // not golden path
        assertThat(isGoldenPath("".split("\""))).isFalse();
        assertThat(isGoldenPath("\"mail\":\"email@domain.com\",\"password\":\"password\"".split("\""))).isFalse();
        assertThat(isGoldenPath("{\"mailmissing quote:\"email@domain.com\",\"password\":\"password\"}".split("\""))).isFalse();
    }

    @Test
    public void jsonString() {
        LoginParser loginParser = new LoginParser(new Json() //
                .put(USER_EMAIL, "email@domain.com") //
                .put(USER_PASSWORD, "password") //
                .toString());

        assertThat(loginParser.getEmail()).isEqualTo("email@domain.com");
        assertThat(loginParser.getPassword()).isEqualTo("password");
    }

    @Test
    public void jsonStringWithEscape() {
        LoginParser loginParser = new LoginParser(new Json() //
                .put(USER_EMAIL, "email@domain.com") //
                .put(USER_PASSWORD, "pa\\u20AC\\u20ACword") //
                .toString());

        assertThat(loginParser.getEmail()).isEqualTo("email@domain.com");
        assertThat(loginParser.getPassword()).isEqualTo("pa\\u20AC\\u20ACword");
    }

    @Test(expected = LoginException.class)
    public void jsonStringInvalidEmailThrowsException() {
        new LoginParser(new Json() //
                .put(USER_EMAIL, "invalid email") //
                .put(USER_PASSWORD, "password") //
                .toString());
    }

    @Test(expected = LoginException.class)
    public void jsonStringPasswordTooShortThrowsException() {
        new LoginParser(new Json() //
                .put(USER_EMAIL, "email@domain.com") //
                .put(USER_PASSWORD, randomAlphabetic(max(0, USER_INFO_MIN_LENGTH - 1))) //
                .toString());
    }

    @Test(expected = LoginException.class)
    public void jsonStringPasswordTooLongThrowsException() {
        new LoginParser(new Json() //
                .put(USER_EMAIL, "email@domain.com") //
                .put(USER_PASSWORD, randomAlphabetic(USER_INFO_MAX_LENGTH + 1)) //
                .toString());
    }

    @Test(expected = JsonException.class)
    public void invalidJsonString() {
        new LoginParser("not json");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyJsonString() {
        new LoginParser("");
    }

    @Test(expected = NullPointerException.class)
    public void nullJsonString() {
        new LoginParser((String) null);
    }
}