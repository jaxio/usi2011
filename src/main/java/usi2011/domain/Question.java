package usi2011.domain;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static usi2011.util.Json.jsonEscape;
import static usi2011.util.Specifications.FIRST_QUESTION;
import static usi2011.util.Specifications.MAX_NUMBER_OF_ANSWERS;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;
import static usi2011.util.Specifications.QUESTION_ANSWER_1;
import static usi2011.util.Specifications.QUESTION_ANSWER_2;
import static usi2011.util.Specifications.QUESTION_ANSWER_3;
import static usi2011.util.Specifications.QUESTION_ANSWER_4;
import static usi2011.util.Specifications.QUESTION_MAX_LENGTH;
import static usi2011.util.Specifications.QUESTION_MIN_LENGTH;
import static usi2011.util.Specifications.QUESTION_QUESTION;
import static usi2011.util.Specifications.QUESTION_SCORE;
import usi2011.exception.GameSessionException;

public final class Question {
    private final Integer questionId;
    private final String label;
    private final String labelJsonEscaped;
    private final Integer goodChoice;
    private final String goodAnswer;
    private final String goodAnswerJsonEscaped;
    private final String choice1;
    private final String choice1JsonEscaped;
    private final String choice2;
    private final String choice2JsonEscaped;
    private final String choice3;
    private final String choice3JsonEscaped;
    private final String choice4;
    private final String choice4JsonEscaped;
    private final String asJsonScoreWithoutScoreValueAndCurlyBracket;

    public Question(int questionId, String label, int goodChoice, String choice1, String choice2, String choice3, String choice4) {
        this.questionId = questionId;
        this.label = label;
        this.labelJsonEscaped = jsonEscape(label);
        this.goodChoice = goodChoice;
        this.choice1 = choice1;
        this.choice1JsonEscaped = jsonEscape(choice1);
        this.choice2 = choice2;
        this.choice2JsonEscaped = jsonEscape(choice2);
        this.choice3 = choice3;
        this.choice3JsonEscaped = jsonEscape(choice3);
        this.choice4 = choice4;
        this.choice4JsonEscaped = jsonEscape(choice4);

        this.goodAnswer = buildGoodAnswer();
        this.goodAnswerJsonEscaped = buildGoodAnswerJsonEscaped();
        this.asJsonScoreWithoutScoreValueAndCurlyBracket = toJsonScoreWithoutScoreValueAndCurlyBracket();
    }

    private String toJsonScoreWithoutScoreValueAndCurlyBracket() {
        final StringBuilder builder = new StringBuilder(256);
        builder.append("{\"").append(QUESTION_QUESTION).append("\":\"").append(labelJsonEscaped);
        builder.append("\",\"").append(QUESTION_ANSWER_1).append("\":\"").append(choice1JsonEscaped);
        builder.append("\",\"").append(QUESTION_ANSWER_2).append("\":\"").append(choice2JsonEscaped);
        builder.append("\",\"").append(QUESTION_ANSWER_3).append("\":\"").append(choice3JsonEscaped);
        builder.append("\",\"").append(QUESTION_ANSWER_4).append("\":\"").append(choice4JsonEscaped);
        builder.append("\",\"").append(QUESTION_SCORE).append("\":\"");
        return builder.toString();
    }

    public String asJsonStringForScore(int score) {
        return asJsonScoreWithoutScoreValueAndCurlyBracket + score + "\"}";
    }

    public boolean isRightAnswer(int myChoice) {
        return goodChoice == myChoice;
    }

    public String getGoodAnswerJsonEscaped() {
        return goodAnswerJsonEscaped;
    }

    private String buildGoodAnswerJsonEscaped() {
        switch (goodChoice) {
        case 1:
            return choice1JsonEscaped;
        case 2:
            return choice2JsonEscaped;
        case 3:
            return choice3JsonEscaped;
        case 4:
            return choice4JsonEscaped;
        default:
            throw new GameSessionException("Choice " + goodChoice + " does not make sense");
        }
    }

    public String getGoodAnswer() {
        return goodAnswer;
    }

    private String buildGoodAnswer() {
        switch (goodChoice) {
        case 1:
            return choice1;
        case 2:
            return choice2;
        case 3:
            return choice3;
        case 4:
            return choice4;
        default:
            throw new GameSessionException("Choice " + goodChoice + " does not make sense");
        }
    }

    public int getQuestionId() {
        return questionId;
    }

    public String getLabel() {
        return label;
    }

    public String getLabelJsonEscaped() {
        return labelJsonEscaped;
    }

    public int getGoodChoice() {
        return goodChoice;
    }

    public String getChoice1() {
        return choice1;
    }

    public String getChoice2() {
        return choice2;
    }

    public String getChoice3() {
        return choice3;
    }

    public String getChoice4() {
        return choice4;
    }

    @Override
    public String toString() {
        return reflectionToString(this, SHORT_PREFIX_STYLE);
    }

    public static final class QuestionBuilder {
        protected Integer questionId;
        protected String label;
        protected Integer goodChoice;
        protected String choice1;
        protected String choice2;
        protected String choice3;
        protected String choice4;
        protected int score = 0;
        protected String asJsonString;

        public QuestionBuilder setQuestionId(Integer questionId) {
            this.questionId = questionId;
            return this;
        }

        public QuestionBuilder setLabel(String label) {
            this.label = label;
            return this;
        }

        public QuestionBuilder setGoodChoice(Integer goodChoice) {
            this.goodChoice = goodChoice;
            return this;
        }

        public QuestionBuilder setChoice1(String choice1) {
            this.choice1 = choice1;
            return this;
        }

        public QuestionBuilder setChoice2(String choice2) {
            this.choice2 = choice2;
            return this;
        }

        public QuestionBuilder setChoice3(String choice3) {
            this.choice3 = choice3;
            return this;
        }

        public QuestionBuilder setChoice4(String choice4) {
            this.choice4 = choice4;
            return this;
        }

        public QuestionBuilder setScore(int score) {
            this.score = score;
            return this;
        }

        public Question build() {
            checkNotNull(questionId, "questionId missing");
            checkState(questionId >= FIRST_QUESTION, "questionId is below the first question " + FIRST_QUESTION);
            checkState(questionId <= MAX_NUMBER_OF_QUESTIONS, "questionId is above the max numnber of question " + MAX_NUMBER_OF_QUESTIONS);
            checkNotNull(trimToNull(label), "label missing");
            checkNotNull(goodChoice, "goodChoice missing");
            checkState(goodChoice >= 1, "goodChoice should start at 1");
            checkState(goodChoice <= MAX_NUMBER_OF_ANSWERS, "goodChoice is above the max number of answers of " + MAX_NUMBER_OF_ANSWERS);
            checkNotNull(choice1, "choice1 missing");
            checkNotNull(choice2, "choice2 missing");
            checkNotNull(choice3, "choice3 missing");
            checkNotNull(choice4, "choice4 missing");
            checkState(!tooSmall(choice1), "choice1 is too small");
            checkState(!tooSmall(choice2), "choice2 is too small");
            checkState(!tooSmall(choice3), "choice3 is too small");
            checkState(!tooSmall(choice4), "choice4 is too small");
            checkState(!tooBig(choice1), "choice1 is too big");
            checkState(!tooBig(choice2), "choice2 is too big");
            checkState(!tooBig(choice3), "choice3 is too big");
            checkState(!tooBig(choice4), "choice4 is too big");

            return new Question(questionId, label, goodChoice, choice1, choice2, choice3, choice4);
        }

        private boolean tooSmall(String value) {
            return value.length() < QUESTION_MIN_LENGTH;
        }

        private boolean tooBig(String value) {
            return value.length() > QUESTION_MAX_LENGTH;
        }
    }
}