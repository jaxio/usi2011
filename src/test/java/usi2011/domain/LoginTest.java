package usi2011.domain;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class LoginTest {
    @Test
    public void valid() {
        Login login = new Login("email@domain.com", "password");
        assertThat(login.getEmail()).isEqualTo("email@domain.com");
        assertThat(login.getPassword()).isEqualTo("password");
    }

    @Test(expected = RuntimeException.class)
    public void nullEmailThrowsException() {
        new Login(null, "password");
    }

    @Test(expected = RuntimeException.class)
    public void invalidEmailThrowsException() {
        new Login("not an email", "password");
    }

    @Test(expected = RuntimeException.class)
    public void nullPasswordThrowsException() {
        new Login("email@domain.com", null);
    }

    @Test(expected = RuntimeException.class)
    public void passwordTooShortThrowsException() {
        new Login("email@domain.com", randomAlphabetic(1));
    }

    @Test(expected = RuntimeException.class)
    public void passwordTooLongThrowsException() {
        new Login("email@domain.com", randomAlphabetic(300));
    }
}
