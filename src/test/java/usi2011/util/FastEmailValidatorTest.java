package usi2011.util;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.util.FastEmailValidator.isEmail;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class FastEmailValidatorTest {
    static class EmailTest {
        public String email;
        public boolean status;

        public EmailTest(String email, boolean status) {
            this.email = email;
            this.status = status;
        }
    }

    @DataPoints
    public static EmailTest[] emails = new EmailTest[] { new EmailTest("a@b.com", true), //
            new EmailTest("florent@ramiere.com", true), //
            new EmailTest("vang.johnson@hotmail.com", true), //
            new EmailTest("info@jaxio.com", true), //
            new EmailTest("aa@dd.com", true), //
            new EmailTest(null, false), //
            new EmailTest("a..", false), //
            new EmailTest("@", false),//
            new EmailTest(".", false), };

    @Theory
    public void testIsEmail(EmailTest test) {
        boolean email = isEmail(test.email);
        System.out.println(test.email + " " + email);
        assertThat(email).isEqualTo(test.status);
    }
}