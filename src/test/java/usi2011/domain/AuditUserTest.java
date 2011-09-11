package usi2011.domain;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.util.Specifications.AUDIT_GOOD_ANSWERS;
import static usi2011.util.Specifications.AUDIT_USER_ANSWERS;
import static usi2011.util.Specifications.FIRST_QUESTION;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;

import org.junit.Test;

public class AuditUserTest {
    @Test
    public void empty() {
        int[] userAnswers = new int[MAX_NUMBER_OF_QUESTIONS + 1];
        int[] goodAnswers = new int[MAX_NUMBER_OF_QUESTIONS + 1];
        String json = new AuditUser(userAnswers, goodAnswers).getJson();
        String expected = "{\"" + AUDIT_USER_ANSWERS + "\":null,\"" + AUDIT_GOOD_ANSWERS + "\":null}";
        assertThat(json).isEqualTo(expected);
    }

    @Test
    public void notEmpty() {
        int[] userAnswers = new int[MAX_NUMBER_OF_QUESTIONS + 1];
        int[] goodAnswers = new int[MAX_NUMBER_OF_QUESTIONS + 1];
        for (int i = FIRST_QUESTION; i <= MAX_NUMBER_OF_QUESTIONS; i++) {
            userAnswers[i] = (i - 1) % 4 + 1;
            goodAnswers[i] = (i - 1) % 4 + 1;
        }

        String json = new AuditUser(userAnswers, goodAnswers).getJson();
        String expected = "{\"" + AUDIT_USER_ANSWERS
                + "\":[\"1\",\"2\",\"3\",\"4\",\"1\",\"2\",\"3\",\"4\",\"1\",\"2\",\"3\",\"4\",\"1\",\"2\",\"3\",\"4\",\"1\",\"2\",\"3\",\"4\"],\""
                + AUDIT_GOOD_ANSWERS
                + "\":[\"1\",\"2\",\"3\",\"4\",\"1\",\"2\",\"3\",\"4\",\"1\",\"2\",\"3\",\"4\",\"1\",\"2\",\"3\",\"4\",\"1\",\"2\",\"3\",\"4\"]}";

        System.out.println(json);
        System.out.println(expected);
        assertThat(json).isEqualTo(expected);
    }

    @Test(expected = NullPointerException.class)
    public void nullUserAnswersThrowsException() {
        new AuditUser(null, new int[MAX_NUMBER_OF_QUESTIONS + 1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongUserAnswersSizeThrowsException() {
        new AuditUser(new int[MAX_NUMBER_OF_QUESTIONS - 1], new int[MAX_NUMBER_OF_QUESTIONS + 1]);
    }

    @Test(expected = NullPointerException.class)
    public void nullGoodAnswersThrowsException() {
        new AuditUser(new int[MAX_NUMBER_OF_QUESTIONS + 1], null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongGoodAnswersSizeThrowsException() {
        new AuditUser(new int[MAX_NUMBER_OF_QUESTIONS + 1], new int[MAX_NUMBER_OF_QUESTIONS - 1]);
    }

}
