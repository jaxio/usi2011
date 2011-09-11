package usi2011.repository;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.Main.context;
import static usi2011.repository.ParameterRepository.ParametersMetadata.FLUSH_USERTABLE;
import static usi2011.repository.ParameterRepository.ParametersMetadata.LOGIN_TIMEOUT_IN_SECONDS;
import static usi2011.repository.ParameterRepository.ParametersMetadata.NB_QUESTIONS;
import static usi2011.repository.ParameterRepository.ParametersMetadata.NB_USERS_THRESHOLD;
import static usi2011.repository.ParameterRepository.ParametersMetadata.QUESTION_TIMEFRAME_IN_SECONDS;
import static usi2011.repository.ParameterRepository.ParametersMetadata.SYNCRO_TIME_IN_SECONDS;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.BeansException;

import usi2011.domain.Parameters;
import usi2011.domain.Parameters.ParametersBuilder;

@Ignore
public class ParameterRepositoryTest {

    static ParameterRepository repository = null;

    @BeforeClass
    public static void init() throws BeansException, IOException {
        repository = context.getBean(ParameterRepository.class);
    }

    @Test
    public void reset() {
        repository.reset();
        assertThat(repository.getParameters()).isNull();
    }

    @Test
    public void saveAndGet() {
        repository.reset();
        assertThat(repository.getParameters()).isNull();

        Parameters expected = parameters();
        repository.save(expected);

        Parameters loaded = repository.getParameters();
        assertThat(loaded).isNotNull();
        assertThat(loaded).isEqualTo(expected);

        repository.reset();

        assertThat(repository.getParameters()).isNull();
        assertThat(repository.get(FLUSH_USERTABLE)).isNull();
        assertThat(repository.get(NB_USERS_THRESHOLD)).isNull();
        assertThat(repository.get(NB_QUESTIONS)).isNull();
        assertThat(repository.get(FLUSH_USERTABLE)).isNull();
        assertThat(repository.get(LOGIN_TIMEOUT_IN_SECONDS)).isNull();
        assertThat(repository.get(SYNCRO_TIME_IN_SECONDS)).isNull();
        assertThat(repository.get(QUESTION_TIMEFRAME_IN_SECONDS)).isNull();
    }

    @Test
    public void get() {
        repository.reset();
        Parameters expected = parameters();
        repository.save(expected);

        assertThat(repository.get(FLUSH_USERTABLE)).isEqualTo(expected.getFlushUserTable());
        assertThat(repository.get(NB_USERS_THRESHOLD)).isEqualTo(expected.getNbUsersThreshold());
        assertThat(repository.get(NB_QUESTIONS)).isEqualTo(expected.getNbQuestions());
        assertThat(repository.get(FLUSH_USERTABLE)).isEqualTo(expected.getFlushUserTable());
        assertThat(repository.get(LOGIN_TIMEOUT_IN_SECONDS)).isEqualTo(expected.getLoginTimeoutInMs());
        assertThat(repository.get(SYNCRO_TIME_IN_SECONDS)).isEqualTo(expected.getSynchrotimeInMs());
        assertThat(repository.get(QUESTION_TIMEFRAME_IN_SECONDS)).isEqualTo(expected.getQuestionTimeframeInMs());
    }

    private Parameters parameters() {
        return new ParametersBuilder() //
                .setFlushUserTable(true) //
                .setLoginTimeoutInSeconds(1) //
                .setNbQuestions(2) //
                .setNbUsersThreshold(3) //
                .setQuestionTimeframeInSeconds(4) //
                .setSynchrotimeInSeconds(5) //
                .build();
    }
}