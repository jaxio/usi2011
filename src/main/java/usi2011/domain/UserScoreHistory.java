package usi2011.domain;

import static usi2011.util.FastIntegerParser.charToInt;
import static usi2011.util.Specifications.FIRST_QUESTION;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;
import static usi2011.util.SplitUtil.split;

import org.apache.commons.lang.StringUtils;

import usi2011.exception.SessionKeyException;

/**
 * A simple data structure to keep track of user history in session cookie.<br>
 * The format is "email:lastname:firstname:a1:a2:a3...:a20" where ai is the answer to question i.<br>
 * When no answer is present ai=0<br>
 * Example: user has answered to questions 2, 4 and 20 (very dummy example)<br>
 * 
 * <code>
 * "nicolas@jaxio.com|Romanetti|Nicolas|01030000000000000004"
 * </code>
 */
public final class UserScoreHistory {
    public static final int UNANSWERED = 0;
    private static final char VALUE_SEPARATOR = '|'; // when using fastest decoder it should not be ':'
    private static final int[] questionValues = buildQuestionValues();
    private static final String unansweredQuestions = StringUtils.rightPad("", MAX_NUMBER_OF_QUESTIONS + 1, "0");

    private static int[] buildQuestionValues() {
        final int[] ret = new int[MAX_NUMBER_OF_QUESTIONS + 1];
        for (int i = 0; i <= MAX_NUMBER_OF_QUESTIONS; i++) {
            ret[i] = questionValue(i);
        }
        return ret;
    }

    private static int questionValue(int questionN) {
        if (questionN <= 5) {
            return 1;
        } else if (questionN <= 10) {
            return 5;
        } else if (questionN <= 15) {
            return 10;
        } else {
            return 15;
        }
    }

    // needed for score calculation
    private final int[] goodAnswers;
    private final int[] answers = new int[MAX_NUMBER_OF_QUESTIONS + 1];

    // what we store in cookie
    private final String email;
    private final String lastName;
    private final String firstName;

    public UserScoreHistory(String email, String lastName, String firstName, int[] goodAnswers) {
        this.email = email;
        this.lastName = lastName;
        this.firstName = firstName;
        this.goodAnswers = goodAnswers;
    }

    public UserScoreHistory(String value, int[] goodAnswers) {
        this.goodAnswers = goodAnswers;

        final String[] values = split(value, VALUE_SEPARATOR, 4);
        email = values[0];
        lastName = values[1];
        firstName = values[2];
        final String answersEncoded = values[3];
        if (answersEncoded.length() != MAX_NUMBER_OF_QUESTIONS + 1) {
            throw new SessionKeyException("Encoded answers is not valid, [" + answersEncoded + "]");
        }
        for (int i = FIRST_QUESTION; i <= MAX_NUMBER_OF_QUESTIONS; i++) {
            answers[i] = charToInt(values[3].charAt(i));
        }
    }

    public String getEmail() {
        return email;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public int getAnswerAsInt(int questionN) {
        return answers[questionN];
    }

    public int getScore() {
        return getScore(MAX_NUMBER_OF_QUESTIONS);
    }

    public int getScore(int questionN) {
        int score = 0;
        int successiveGoodAnswers = 0;

        for (int i = FIRST_QUESTION; i <= questionN && i <= goodAnswers.length; i++) {
            int scoreQuestion = goodAnswers[i] == answers[i] ? questionValues[i] : 0;
            if (scoreQuestion > 0) {
                successiveGoodAnswers++;
                scoreQuestion += bonus(successiveGoodAnswers);
            } else {
                successiveGoodAnswers = 0;
            }
            score += scoreQuestion;
        }
        return score;
    }

    private int bonus(int successiveGoodAnswers) {
        return successiveGoodAnswers <= 1 ? 0 : successiveGoodAnswers - 1;
    }

    /**
     * Do not change the format... this is the one used in constructor.
     */
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append(email).append(VALUE_SEPARATOR);
        sb.append(lastName).append(VALUE_SEPARATOR);
        sb.append(firstName).append(VALUE_SEPARATOR);
        for (int i = 0; i <= MAX_NUMBER_OF_QUESTIONS; i++) {
            sb.append(answers[i]);
        }
        return sb.toString();
    }

    public static final String buildForLogin(String email, String lastName, String firstName) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(email).append(VALUE_SEPARATOR);
        sb.append(lastName).append(VALUE_SEPARATOR);
        sb.append(firstName).append(VALUE_SEPARATOR);
        sb.append(unansweredQuestions);
        return sb.toString();
    }

    public void answer(Question question, int userAnswer) {
        answers[question.getQuestionId()] = userAnswer;
    }

    public boolean userHasAlreadyAnsweredQuestion(Question question) {
        return UNANSWERED != answers[question.getQuestionId()];
    }
}