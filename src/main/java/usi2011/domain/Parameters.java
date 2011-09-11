package usi2011.domain;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.builder.HashCodeBuilder.reflectionHashCode;
import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;

public final class Parameters implements Comparable<Parameters> {
    protected final long loginTimeoutInMs;
    protected final long questionTimeframeInMs;
    protected final long synchrotimeInMs;
    protected final int nbUsersThreshold;
    protected final int nbQuestions;
    protected final boolean flushUserTable;

    public Parameters(Integer nbUsersThreshold, Integer loginTimeoutInSeconds, Integer questionTimeframeInSeconds, Integer synchrotimeInSeconds,
            Integer nbQuestions, Boolean flushUserTable) {
        this.loginTimeoutInMs = loginTimeoutInSeconds * 1000;
        this.questionTimeframeInMs = questionTimeframeInSeconds * 1000;
        this.synchrotimeInMs = synchrotimeInSeconds * 1000;
        this.nbUsersThreshold = nbUsersThreshold;
        this.nbQuestions = nbQuestions;
        this.flushUserTable = flushUserTable;
    }

    public Long getLoginTimeoutInMs() {
        return loginTimeoutInMs;
    }

    public Long getSynchrotimeInMs() {
        return synchrotimeInMs;
    }

    public Integer getNbUsersThreshold() {
        return nbUsersThreshold;
    }

    public Long getQuestionTimeframeInMs() {
        return questionTimeframeInMs;
    }

    public Integer getNbQuestions() {
        return nbQuestions;
    }

    public Boolean getFlushUserTable() {
        return flushUserTable;
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }

    @Override
    public int compareTo(Parameters other) {
        return other == null ? -1 : hashCode() - other.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return compareTo((Parameters) other) == 0;
    }

    @Override
    public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }

    public static final class ParametersBuilder {
        protected Integer loginTimeoutInSeconds;
        protected Integer synchrotimeInSeconds;
        protected Integer nbUsersThreshold;
        protected Integer questionTimeframeInSeconds;
        protected Integer nbQuestions;
        protected Boolean flushUserTable;

        public ParametersBuilder setLoginTimeoutInSeconds(Integer loginTimeoutInSeconds) {
            this.loginTimeoutInSeconds = loginTimeoutInSeconds;
            return this;
        }

        public ParametersBuilder setSynchrotimeInSeconds(Integer synchrotimeInSeconds) {
            this.synchrotimeInSeconds = synchrotimeInSeconds;
            return this;
        }

        public ParametersBuilder setNbUsersThreshold(Integer nbUsersThreshold) {
            this.nbUsersThreshold = nbUsersThreshold;
            return this;
        }

        public ParametersBuilder setQuestionTimeframeInSeconds(Integer questionTimeframeInSeconds) {
            this.questionTimeframeInSeconds = questionTimeframeInSeconds;
            return this;
        }

        public ParametersBuilder setNbQuestions(Integer nbQuestions) {
            this.nbQuestions = nbQuestions;
            return this;
        }

        public ParametersBuilder setFlushUserTable(Boolean flushUserTable) {
            this.flushUserTable = flushUserTable;
            return this;
        }

        public Parameters build() {
            checkNotNull(loginTimeoutInSeconds, "loginTimeoutInSeconds missing");
            checkNotNull(synchrotimeInSeconds, "synchrotimeInSeconds missing");
            checkNotNull(nbUsersThreshold, "nbUsersThreshold missing");
            checkNotNull(questionTimeframeInSeconds, "questionTimeframeInSeconds missing");
            checkNotNull(nbQuestions, "nbQuestions missing");
            checkState(nbQuestions > 1, "there should be at least 1 question, got %s", nbQuestions);
            checkState(nbQuestions <= MAX_NUMBER_OF_QUESTIONS, "there is more than the maximum number of questions possible (%s), got %s",
                    MAX_NUMBER_OF_QUESTIONS, nbQuestions);
            checkNotNull(flushUserTable, "flushusertable missing");
            return new Parameters(nbUsersThreshold, loginTimeoutInSeconds, questionTimeframeInSeconds, synchrotimeInSeconds, nbQuestions, flushUserTable);
        }
    }
}