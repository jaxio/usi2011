package usi2011.domain;

import static java.lang.String.format;
import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.fest.assertions.Assertions.assertThat;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;

import java.util.Arrays;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import usi2011.domain.Question.QuestionBuilder;

@RunWith(Theories.class)
public class UserScoreHistoryTheoryTest {
    public static int GOOD_ANSWER = 1;
    public static int WRONG_ANSWER = 2;

    public static class ScoreValue {
        int questionId;
        int points;
        int answer;
        int successiveGoodAnswers;
        int bonus;
        int score;

        public ScoreValue(int questionId, int points, int answer, int successiveGoodAnswers, int bonus, int score) {
            super();
            this.questionId = questionId;
            this.points = points;
            this.answer = answer;
            this.successiveGoodAnswers = successiveGoodAnswers;
            this.bonus = bonus;
            this.score = score;
        }

        public String toString() {
            return reflectionToString(this, SHORT_PREFIX_STYLE);
        }
    }

    @DataPoints
    public static ScoreValue[] scores = new ScoreValue[] { //
    new ScoreValue(1, 1, WRONG_ANSWER, 0, 0, 0) //
            , new ScoreValue(2, 1, GOOD_ANSWER, 1, 0, 1) //
            , new ScoreValue(3, 1, GOOD_ANSWER, 2, 1, 3) //
            , new ScoreValue(4, 1, WRONG_ANSWER, 0, 0, 3) //
            , new ScoreValue(5, 1, GOOD_ANSWER, 1, 0, 4) //
            , new ScoreValue(6, 5, GOOD_ANSWER, 2, 1, 10) //
            , new ScoreValue(7, 5, GOOD_ANSWER, 3, 2, 17) //
            , new ScoreValue(8, 5, WRONG_ANSWER, 0, 0, 17) //
            , new ScoreValue(9, 5, GOOD_ANSWER, 1, 0, 22) //
            , new ScoreValue(10, 5, WRONG_ANSWER, 0, 0, 22) //
            , new ScoreValue(11, 10, WRONG_ANSWER, 0, 0, 22) //
            , new ScoreValue(12, 10, WRONG_ANSWER, 0, 0, 22) //
            , new ScoreValue(13, 10, WRONG_ANSWER, 0, 0, 22) //
            , new ScoreValue(14, 10, GOOD_ANSWER, 1, 0, 32) //
            , new ScoreValue(15, 10, WRONG_ANSWER, 0, 0, 32) //
            , new ScoreValue(16, 15, GOOD_ANSWER, 1, 0, 47) //
            , new ScoreValue(17, 15, WRONG_ANSWER, 0, 0, 47) //
            , new ScoreValue(18, 15, GOOD_ANSWER, 1, 0, 62) //
            , new ScoreValue(19, 15, GOOD_ANSWER, 2, 1, 78) //
            , new ScoreValue(20, 15, GOOD_ANSWER, 3, 2, 95) };

    private static int[] goodAnswers = new int[MAX_NUMBER_OF_QUESTIONS + 1];
    static {
        Arrays.fill(goodAnswers, GOOD_ANSWER);
    }

    private static UserScoreHistory userScoreHistory = new UserScoreHistory("email@domain.com", "ln", "fn", goodAnswers);

    @Theory
    public void filenameIncludesUsername(ScoreValue scoreValue) {
        Question question = new QuestionBuilder() //
                .setQuestionId(scoreValue.questionId) //
                .setGoodChoice(GOOD_ANSWER) //
                .setLabel("question") //
                .setChoice1("choice1") //
                .setChoice2("choice2") //
                .setChoice3("choice3") //
                .setChoice4("choice4") //
                .build();

        userScoreHistory.answer(question, scoreValue.answer);

        int myScore = userScoreHistory.getScore();

        System.out.println("Answering question " + format("%02d", scoreValue.questionId) + " with " + (scoreValue.answer == WRONG_ANSWER ? "wrong" : "good ")
                + " answer " + userScoreHistory.toString());
        assertThat(myScore).isEqualTo(scoreValue.score);
        assertThat(userScoreHistory.getScore(scoreValue.questionId)).isEqualTo(scoreValue.score);
    }
}