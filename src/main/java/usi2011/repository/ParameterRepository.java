package usi2011.repository;

import usi2011.domain.Parameters;

public interface ParameterRepository {
    public static enum ParametersMetadata {
        SYNCRO_TIME_IN_SECONDS, NB_USERS_THRESHOLD, LOGIN_TIMEOUT_IN_SECONDS, QUESTION_TIMEFRAME_IN_SECONDS, NB_QUESTIONS, FLUSH_USERTABLE, GOOD_ANSWERS_AS_COMMA_SEPARATED_VALUES
    }

    void reset();

    void save(Parameters parameters);

    Parameters getParameters();

    <T> T get(ParametersMetadata key);
}
