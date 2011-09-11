package usi2011.domain;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.domain.UserScoreHistory.UNANSWERED;
import static usi2011.util.Specifications.FIRST_QUESTION;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;

import org.junit.Ignore;
import org.junit.Test;

import usi2011.exception.SessionKeyException;

public class UserScoreHistoryTest {

    private static final int[] goodAnswers = new int[] { UNANSWERED, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4 }; // MAX_NUMBER_OF_ANSWERS
                                                                                                                                   // answers...

    @Test
    public void email() {
        UserScoreHistory cookieValue = new UserScoreHistory("email@domain.com", "ln", "fn", goodAnswers);

        assertThat(cookieValue.getEmail()).isEqualTo("email@domain.com");
        assertThat(cookieValue.getLastName()).isEqualTo("ln");
        assertThat(cookieValue.getFirstName()).isEqualTo("fn");
        assertThat(cookieValue.getScore()).isEqualTo(0);
        for (int i = FIRST_QUESTION; i <= MAX_NUMBER_OF_QUESTIONS; i++) {
            assertThat(cookieValue.getScore(i)).isEqualTo(0);
        }
    }

    @Test
    public void multipleAnswers() {
        UserScoreHistory cookieValue = new UserScoreHistory("email@domain.com", "ln", "fn", goodAnswers);
        assertThat(cookieValue.getEmail()).isEqualTo("email@domain.com");
        assertThat(cookieValue.getLastName()).isEqualTo("ln");
        assertThat(cookieValue.getFirstName()).isEqualTo("fn");
        assertThat(cookieValue.getScore()).isEqualTo(0);

        cookieValue.answer(question(1), goodAnswers[1]);

        assertThat(cookieValue.getScore()).isEqualTo(1);

        cookieValue.answer(question(2), goodAnswers[2]);

        assertThat(cookieValue.getScore()).isEqualTo(3);
        cookieValue.answer(question(3), goodAnswers[1]); // WRONG ANSWER on purpose

        assertThat(cookieValue.getScore()).isEqualTo(3);
    }

    @Test
    public void scoreWhen1Good1NotAnswered1Good() {
        UserScoreHistory cookieValue = new UserScoreHistory("email@domain.com", "ln", "fn", goodAnswers);
        assertThat(cookieValue.getScore()).isEqualTo(0);
        cookieValue.answer(question(1), goodAnswers[1]);
        cookieValue.answer(question(3), goodAnswers[3]);
        assertThat(cookieValue.getScore()).isEqualTo(2);
    }

    @Test
    public void decode() {
        UserScoreHistory cookieValue = new UserScoreHistory("florent@ramiere.com|ramiere|florent|000000000000000000000", goodAnswers);
        assertThat(cookieValue.getEmail()).isEqualTo("florent@ramiere.com");
        assertThat(cookieValue.getLastName()).isEqualTo("ramiere");
        assertThat(cookieValue.getFirstName()).isEqualTo("florent");
        assertThat(cookieValue.getScore()).isEqualTo(0);
    }
    
    @Test
    public void perfectAnswers() {
        UserScoreHistory cookieValue = new UserScoreHistory("florent@ramiere.com|ramiere|florent|012341234123412341234", goodAnswers);
        assertThat(cookieValue.getScore()).isEqualTo(345);
    }

    @Test(expected = SessionKeyException.class)
    @Ignore
    public void wrongUserAnswersLengthThrowsException() {
        new UserScoreHistory("florent@ramiere.com|ramiere|florent:000000", goodAnswers);
    }

    @Test(expected = SessionKeyException.class)
    public void invalidEmailThrowsException() {
        new UserScoreHistory("not an email|ramiere|florent|000000", goodAnswers);
    }

    @Test(expected = SessionKeyException.class)
    @Ignore
    public void tooManyElementsThrowsException() {
        new UserScoreHistory("florent@ramiere.com|ramiere|florent|000000|extra", goodAnswers);
    }

    @Test(expected = SessionKeyException.class)
    @Ignore
    public void tooFewElementsThrowsException() {
        new UserScoreHistory("missing-email-ramiere|florent|000000", goodAnswers);
    }

    private Question question(int questionId) {
        return new Question.QuestionBuilder() //
                .setQuestionId(questionId) //
                .setLabel("label") //
                .setGoodChoice(goodAnswers[questionId]) //
                .setChoice1("choice1") //
                .setChoice2("choice2") //
                .setChoice3("good answer") //
                .setChoice4("choice4") //
                .build();
    }
}