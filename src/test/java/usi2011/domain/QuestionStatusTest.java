package usi2011.domain;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;

import org.junit.Test;

import usi2011.domain.QuestionStatus.QuestionStatusBuilder;

public class QuestionStatusTest {
    @Test
    public void valid() {
        QuestionStatus questionStatus = questionStatusBuilder().build();
        assertThat(questionStatus.getQuestionId()).isEqualTo(1);
        assertThat(questionStatus.getWhenQuestionTimeframeStartedInMilliseconds()).isEqualTo(1);
        assertThat(questionStatus.questionTimeframeStarted()).isTrue();
        assertThat(questionStatus.getWhenSynchrotimeStartedInMilliseconds()).isEqualTo(2);
        assertThat(questionStatus.synchrotimeStarted()).isTrue();
    }

    @Test
    public void validBoundaries() {
        questionStatusBuilder().setQuestionId(1).build();
        questionStatusBuilder().setQuestionId(MAX_NUMBER_OF_QUESTIONS).build();
    }

    @Test(expected = IllegalStateException.class)
    public void questionIdIsTooLowThrowsException() {
        questionStatusBuilder().setQuestionId(0).build();
    }

    @Test(expected = IllegalStateException.class)
    public void questionIdAboveMaxiumumQuestionsThrowsException() {
        questionStatusBuilder().setQuestionId(MAX_NUMBER_OF_QUESTIONS + 1).build();
    }

    private QuestionStatusBuilder questionStatusBuilder() {
        return new QuestionStatusBuilder() //
                .setQuestionId(1) //
                .setWhenQuestionTimeframeStartedInMilliseconds(1) //
                .setWhenSynchrotimeStartedInMilliseconds(2);
    }
}