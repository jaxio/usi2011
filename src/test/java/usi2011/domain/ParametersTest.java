package usi2011.domain;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import usi2011.domain.Parameters.ParametersBuilder;

public class ParametersTest {
    @Test
    public void builder() {
        Parameters parameters = parametersBuilder().build();

        assertThat(parameters.getFlushUserTable()).isEqualTo(true);
        assertThat(parameters.getLoginTimeoutInMs()).isEqualTo(1000);
        assertThat(parameters.getNbQuestions()).isEqualTo(2);
        assertThat(parameters.getNbUsersThreshold()).isEqualTo(3);
        assertThat(parameters.getQuestionTimeframeInMs()).isEqualTo(4000);
        assertThat(parameters.getSynchrotimeInMs()).isEqualTo(5000);
    }

    @Test(expected = NullPointerException.class)
    public void emptyFlushusertableThrowsException() {
        new ParametersBuilder().setFlushUserTable(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void emptyLogintimeoutThrowsException() {
        new ParametersBuilder().setLoginTimeoutInSeconds(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void emptyNbquestionsThrowsException() {
        new ParametersBuilder().setNbQuestions(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void emptyNbusersthresholdThrowsException() {
        new ParametersBuilder().setNbUsersThreshold(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void emptyQuestiontimeframeThrowsException() {
        new ParametersBuilder().setQuestionTimeframeInSeconds(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void emptySynchrotimeThrowsException() {
        new ParametersBuilder().setSynchrotimeInSeconds(null).build();
    }

    private ParametersBuilder parametersBuilder() {
        return new ParametersBuilder() //
                .setFlushUserTable(true) //
                .setLoginTimeoutInSeconds(1) //
                .setNbQuestions(2) //
                .setNbUsersThreshold(3) //
                .setQuestionTimeframeInSeconds(4) //
                .setSynchrotimeInSeconds(5);
    }
}