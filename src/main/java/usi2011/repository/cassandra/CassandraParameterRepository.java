package usi2011.repository.cassandra;

import static java.lang.Boolean.parseBoolean;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.repository.ParameterRepository.ParametersMetadata.FLUSH_USERTABLE;
import static usi2011.repository.ParameterRepository.ParametersMetadata.LOGIN_TIMEOUT_IN_SECONDS;
import static usi2011.repository.ParameterRepository.ParametersMetadata.NB_QUESTIONS;
import static usi2011.repository.ParameterRepository.ParametersMetadata.NB_USERS_THRESHOLD;
import static usi2011.repository.ParameterRepository.ParametersMetadata.QUESTION_TIMEFRAME_IN_SECONDS;
import static usi2011.repository.ParameterRepository.ParametersMetadata.SYNCRO_TIME_IN_SECONDS;
import static usi2011.repository.cassandra.CassandraRepository.cfName;
import static usi2011.repository.cassandra.CassandraRepository.createStringColumn;
import static usi2011.util.FastIntegerParser.parseInt;
import static usi2011.util.LogUtil.isDebugEnabled;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.Profiles.HECTOR;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.ColumnSlice;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import usi2011.domain.Parameters;
import usi2011.domain.Parameters.ParametersBuilder;
import usi2011.repository.ParameterRepository;

@Component
@ManagedResource
@Profile(HECTOR)
public class CassandraParameterRepository implements ParameterRepository {
    private static final Logger logger = getLogger(CassandraQuestionRepository.class);
    private static final StringSerializer stringSerializer = StringSerializer.get();
    private static final String COLUMN_FAMILY = cfName(ParametersMetadata.class);
    private static final String PARAMETERS_KEY = "values";

    @Autowired
    private CassandraRepository cassandra;

    @Override
    @ManagedOperation
    public void reset() {
        cassandra.truncate(COLUMN_FAMILY);
    }

    @Override
    public void save(final Parameters parameters) {
        createMutator(cassandra.keyspace(), stringSerializer)
                .addInsertion(PARAMETERS_KEY, COLUMN_FAMILY, createStringColumn(LOGIN_TIMEOUT_IN_SECONDS.name(), parameters.getLoginTimeoutInMs() / 1000))
                .addInsertion(PARAMETERS_KEY, COLUMN_FAMILY,
                        createStringColumn(QUESTION_TIMEFRAME_IN_SECONDS.name(), parameters.getQuestionTimeframeInMs() / 1000))
                .addInsertion(PARAMETERS_KEY, COLUMN_FAMILY, createStringColumn(SYNCRO_TIME_IN_SECONDS.name(), parameters.getSynchrotimeInMs() / 1000))
                .addInsertion(PARAMETERS_KEY, COLUMN_FAMILY, createStringColumn(NB_USERS_THRESHOLD.name(), parameters.getNbUsersThreshold()))
                .addInsertion(PARAMETERS_KEY, COLUMN_FAMILY, createStringColumn(NB_QUESTIONS.name(), parameters.getNbQuestions()))
                .addInsertion(PARAMETERS_KEY, COLUMN_FAMILY, createStringColumn(FLUSH_USERTABLE.name(), parameters.getFlushUserTable())) //
                .execute();
        if (isDebugEnabled) {
            logger.debug("saved parameters");
        }
    }

    @Override
    public Parameters getParameters() {
        final ColumnSlice<String, String> result = createSliceQuery(cassandra.keyspace(), stringSerializer, stringSerializer, stringSerializer)
                .setColumnFamily(COLUMN_FAMILY) //
                .setKey(PARAMETERS_KEY) //
                .setColumnNames(LOGIN_TIMEOUT_IN_SECONDS.name(), QUESTION_TIMEFRAME_IN_SECONDS.name() //
                        , SYNCRO_TIME_IN_SECONDS.name(), NB_USERS_THRESHOLD.name() //
                        , NB_QUESTIONS.name(), FLUSH_USERTABLE.name()) //
                .execute() //
                .get();
        if (result.getColumns().isEmpty()) {
            if (isInfoEnabled) {
                logger.info("No parameters in repository");
            }
            return null;
        }
        if (isInfoEnabled) {
            logger.info("Parameters loaded");
        }
        return new ParametersBuilder() //
                .setLoginTimeoutInSeconds(parseInt(result.getColumnByName(LOGIN_TIMEOUT_IN_SECONDS.name()).getValue())) //
                .setQuestionTimeframeInSeconds(parseInt(result.getColumnByName(QUESTION_TIMEFRAME_IN_SECONDS.name()).getValue())) //
                .setSynchrotimeInSeconds(parseInt(result.getColumnByName(SYNCRO_TIME_IN_SECONDS.name()).getValue())) //
                .setNbUsersThreshold(parseInt(result.getColumnByName(NB_USERS_THRESHOLD.name()).getValue())) //
                .setNbQuestions(parseInt(result.getColumnByName(NB_QUESTIONS.name()).getValue())) //
                .setFlushUserTable(parseBoolean(result.getColumnByName(FLUSH_USERTABLE.name()).getValue())) //
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(ParametersMetadata key) {
        Parameters parameters = getParameters();
        if (parameters == null) {
            return null;
        }
        switch (key) {
        case LOGIN_TIMEOUT_IN_SECONDS:
            return (T) parameters.getLoginTimeoutInMs();
        case QUESTION_TIMEFRAME_IN_SECONDS:
            return (T) parameters.getQuestionTimeframeInMs();
        case SYNCRO_TIME_IN_SECONDS:
            return (T) parameters.getSynchrotimeInMs();
        case NB_USERS_THRESHOLD:
            return (T) parameters.getNbUsersThreshold();
        case NB_QUESTIONS:
            return (T) parameters.getNbQuestions();
        case FLUSH_USERTABLE:
            return (T) parameters.getFlushUserTable();
        default:
            throw new IllegalArgumentException(key + " is not valid");
        }
    }
}