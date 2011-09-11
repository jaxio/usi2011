package usi2011.domain;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.fest.assertions.Assertions.assertThat;
import static usi2011.util.Json.jsonEscape;

import org.junit.Test;

public class UserTest {
    @Test
    public void valid() {
        User user = new User("email@domain.com", "firstName", "lastName", "password");

        assertThat(user.getEmail()).isEqualTo("email@domain.com");
        assertThat(user.getFirstName()).isEqualTo("firstName");
        assertThat(user.getLastName()).isEqualTo("lastName");
        assertThat(user.getPassword()).isEqualTo("password");
        assertThat(user.getFirstNameJsonEscaped()).isEqualTo(jsonEscape(user.getFirstName()));
        assertThat(user.getLastNameJsonEscaped()).isEqualTo(jsonEscape(user.getLastName()));
        assertThat(user.getPasswordJsonEscaped()).isEqualTo(jsonEscape(user.getPassword()));
    }

    @Test
    public void validWithEncoding() {
        User user = new User("email@domain.com", "firstname with Euro sign {\u20AC}", "lastName with tab [\t]", "password");

        assertThat(user.getEmail()).isEqualTo("email@domain.com");
        assertThat(user.getFirstName()).isEqualTo("firstname with Euro sign {\u20AC}");
        assertThat(user.getLastName()).isEqualTo("lastName with tab [\t]");
        assertThat(user.getPassword()).isEqualTo("password");
        assertThat(user.getFirstNameJsonEscaped()).isEqualTo(jsonEscape(user.getFirstName()));
        assertThat(user.getLastNameJsonEscaped()).isEqualTo(jsonEscape(user.getLastName()));
        assertThat(user.getPasswordJsonEscaped()).isEqualTo(jsonEscape(user.getPassword()));
    }

    @Test
    public void user() {
        User user = new User("email@domain.com", "firstname with Euro sign {\u20AC}", "lastName with tab [\t]", "normal password");
        User constructed = new User( //
                user.getEmail(), user.getEmailJsonEscaped(), //
                user.getFirstName(), user.getFirstNameJsonEscaped(), //
                user.getLastName(), user.getLastNameJsonEscaped(), //
                user.getPassword(), user.getPasswordJsonEscaped(), null);

        assertThat(constructed.getEmail()).isEqualTo(user.getEmail());
        assertThat(constructed.getEmailJsonEscaped()).isEqualTo(user.getEmailJsonEscaped());
        assertThat(constructed.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(constructed.getFirstNameJsonEscaped()).isEqualTo(user.getFirstNameJsonEscaped());
        assertThat(constructed.getLastName()).isEqualTo(user.getLastName());
        assertThat(constructed.getLastNameJsonEscaped()).isEqualTo(user.getLastNameJsonEscaped());
        assertThat(constructed.getPassword()).isEqualTo(user.getPassword());
        assertThat(constructed.getPasswordJsonEscaped()).isEqualTo(user.getPasswordJsonEscaped());
    }

    @Test
    public void userWhereNoJsonEscapeWasNeccessary() {
        User user = new User("email@domain.com", "firstname", "lastName", "password");
        User constructed = new User( //
                user.getEmail(), null, //
                user.getFirstName(), null, //
                user.getLastName(), null, //
                user.getPassword(), null, null);

        assertThat(constructed.getEmail()).isEqualTo(user.getEmail());
        assertThat(constructed.getEmailJsonEscaped()).isEqualTo(user.getEmailJsonEscaped());
        assertThat(constructed.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(constructed.getFirstNameJsonEscaped()).isEqualTo(user.getFirstNameJsonEscaped());
        assertThat(constructed.getLastName()).isEqualTo(user.getLastName());
        assertThat(constructed.getLastNameJsonEscaped()).isEqualTo(user.getLastNameJsonEscaped());
        assertThat(constructed.getPassword()).isEqualTo(user.getPassword());
        assertThat(constructed.getPasswordJsonEscaped()).isEqualTo(user.getPasswordJsonEscaped());
    }

    @Test(expected = RuntimeException.class)
    public void invalidEmailThrowsException() {
        new User("not an email", "firstName", "lastName", "password");
    }

    @Test(expected = RuntimeException.class)
    public void firstNameTooShortThrowsException() {
        new User("email@domain.com", "f", "lastName", "password");
    }

    @Test(expected = RuntimeException.class)
    public void lastNameTooShortThrowsException() {
        new User("email@domain.com", "firstName", "l", "password");
    }

    @Test(expected = RuntimeException.class)
    public void passwordTooShortThrowsException() {
        new User("email@domain.com", "firstName", "lastName", "p");
    }

    @Test(expected = RuntimeException.class)
    public void firstNameTooLongThrowsException() {
        new User("email@domain.com", randomAlphabetic(400), "lastName", "password");
    }

    @Test(expected = RuntimeException.class)
    public void lastNameTooLongThrowsException() {
        new User("email@domain.com", "firstName", randomAlphabetic(400), "password");
    }

    @Test(expected = RuntimeException.class)
    public void passwordTooLongThrowsException() {
        new User("email@domain.com", "firstName", "lastName", randomAlphabetic(400));
    }
}