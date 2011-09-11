package usi2011.domain;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.apache.commons.lang.builder.HashCodeBuilder.reflectionHashCode;
import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;
import static usi2011.util.Specifications.FIRST_QUESTION;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;

public class QuestionStatus {
    public static final long NOT_STARTED = currentTimeMillis() + DAYS.toMillis(100);
    int questionId = -1;
    long whenQuestionTimeframeStartedInMilliseconds = NOT_STARTED;
    long whenSynchrotimeStartedInMilliseconds = NOT_STARTED;

    QuestionStatus() {
    }

    public QuestionStatus(int questionId, long whenQuestionTimeframeStartedInMilliseconds, long whenSynchrotimeStartedInMilliseconds) {
        super();
        this.questionId = questionId;
        this.whenQuestionTimeframeStartedInMilliseconds = whenQuestionTimeframeStartedInMilliseconds;
        this.whenSynchrotimeStartedInMilliseconds = whenSynchrotimeStartedInMilliseconds;
    }

    public int getQuestionId() {
        return questionId;
    }

    public long getWhenQuestionTimeframeStartedInMilliseconds() {
        return whenQuestionTimeframeStartedInMilliseconds;
    }

    public boolean questionTimeframeStarted() {
        return getWhenQuestionTimeframeStartedInMilliseconds() != NOT_STARTED;
    }

    public long getWhenSynchrotimeStartedInMilliseconds() {
        return whenSynchrotimeStartedInMilliseconds;
    }

    public boolean synchrotimeStarted() {
        return getWhenSynchrotimeStartedInMilliseconds() != NOT_STARTED;
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }

    public static class QuestionStatusBuilder extends QuestionStatus {
        public QuestionStatusBuilder setQuestionId(int questionId) {
            this.questionId = questionId;
            return this;
        }

        public QuestionStatusBuilder setWhenQuestionTimeframeStartedInMilliseconds(long whenQuestionTimeframeStartedInMilliseconds) {
            this.whenQuestionTimeframeStartedInMilliseconds = whenQuestionTimeframeStartedInMilliseconds;
            return this;
        }

        public QuestionStatusBuilder setWhenSynchrotimeStartedInMilliseconds(long whenSynchrotimeStartedInMilliseconds) {
            this.whenSynchrotimeStartedInMilliseconds = whenSynchrotimeStartedInMilliseconds;
            return this;
        }

        public QuestionStatus build() {
            checkState(questionId >= FIRST_QUESTION, "questionId before " + FIRST_QUESTION);
            checkState(questionId <= MAX_NUMBER_OF_QUESTIONS, "questionId is after" + MAX_NUMBER_OF_QUESTIONS);
            return new QuestionStatus(questionId, whenQuestionTimeframeStartedInMilliseconds, whenSynchrotimeStartedInMilliseconds);
        }
    }
}