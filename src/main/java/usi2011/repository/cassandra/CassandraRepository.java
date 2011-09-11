package usi2011.repository.cassandra;

import static me.prettyprint.cassandra.service.OperationType.READ;
import static me.prettyprint.cassandra.service.OperationType.WRITE;
import static me.prettyprint.hector.api.factory.HFactory.createColumnQuery;
import static me.prettyprint.hector.api.factory.HFactory.createKeyspace;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.getOrCreateCluster;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.LogUtil.isDebugEnabled;
import static usi2011.util.LogUtil.isWarnEnabled;
import static usi2011.util.Profiles.HECTOR;

import java.util.List;

import javax.annotation.PostConstruct;

import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ExhaustedPolicy;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.access.MBeanProxyFactoryBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import usi2011.exception.GameSessionException;
import usi2011.repository.GameStatusRepository.GameStatusMetadata;
import usi2011.repository.GameStatusRepository.GameStatusSuperColMetadata;
import usi2011.repository.LoginStatusRepository.LoginStatusMetadata;
import usi2011.repository.ParameterRepository.ParametersMetadata;
import usi2011.repository.QuestionRepository.QuestionsMetadata;
import usi2011.repository.ScoreRepository.AnswersByMailMetadata;
import usi2011.repository.ScoreRepository.FinalScoresMetadata;
import usi2011.repository.UserRepository.UsersMetadata;
import usi2011.util.SplitUtil;

@Component
@ManagedResource
@Profile(HECTOR)
public class CassandraRepository {
    private static final Logger logger = getLogger(CassandraRepository.class);
    private static final StringSerializer stringSerializer = StringSerializer.get();
    private static final String VALUES = "values";

    @Value("${cassandra.cluster:Test Cluster}")
    private String cassandraCluster;

    @Value("${cassandra.keyspace:USI_KEYSPACE}")
    private String cassandraKeySpace;

    @Value("${cassandra.rankingKeyspace:USI_RANKING}")
    private String rankingKeyspaceName;

    @Value("${cassandraRepository.hector.cassandraHosts:localhost}")
    private String cassandraHosts;
    private String[] cassandraHostsArray;

    @Value("${cassandraRepository.hector.cassandraPort:9160}")
    private int cassandraPort;
    @Value("${cassandra.jmx.port}")
    private int cassandraJmxPort;

    @Value("${cassandraRepository.hector.maxActiveConnections}")
    private int maxActiveConnections;
    @Value("${cassandraRepository.hector.autoDiscoverHosts:false}")
    private boolean autoDiscoverHosts;
    @Value("${cassandraRepository.hector.autoDiscoveryDelayInSeconds:30}")
    private int autoDiscoveryDelayInSeconds;

    @Value("${cassandraRepository.hector.useDefaultConsistencyLevel}")
    private boolean useDefaultConsistencyLevel;

    // GAME STATUS
    @Value("${cassandraRepository.hector.gameStatusReadConsistencyLevel}")
    private HConsistencyLevel gameStatusReadConsistencyLevel;
    @Value("${cassandraRepository.hector.gameStatusWriteConsistencyLevel}")
    private HConsistencyLevel gameStatusWriteConsistencyLevel;

    // PARAMETERS
    @Value("${cassandraRepository.hector.parametersReadConsistencyLevel}")
    private HConsistencyLevel parametersReadConsistencyLevel;
    @Value("${cassandraRepository.hector.parametersWriteConsistencyLevel}")
    private HConsistencyLevel parametersWriteConsistencyLevel;

    // QUESTIONS
    @Value("${cassandraRepository.hector.questionsReadConsistencyLevel}")
    private HConsistencyLevel questionsReadConsistencyLevel;
    @Value("${cassandraRepository.hector.questionsWriteConsistencyLevel}")
    private HConsistencyLevel questionsWriteConsistencyLevel;

    // ANSWERS BY MAIL
    @Value("${cassandraRepository.hector.answersByMailReadConsistencyLevel}")
    private HConsistencyLevel answersByMailReadConsistencyLevel;
    @Value("${cassandraRepository.hector.answersByMailWriteConsistencyLevel}")
    private HConsistencyLevel answersByMailWriteConsistencyLevel;

    // USERS
    @Value("${cassandraRepository.hector.usersReadConsistencyLevel}")
    private HConsistencyLevel usersReadConsistencyLevel;
    @Value("${cassandraRepository.hector.usersWriteConsistencyLevel}")
    private HConsistencyLevel usersWriteConsistencyLevel;

    // LOGIN STATUS
    @Value("${cassandraRepository.hector.loginStatusReadConsistencyLevel}")
    private HConsistencyLevel loginStatusReadConsistencyLevel;
    @Value("${cassandraRepository.hector.loginStatusWriteConsistencyLevel}")
    private HConsistencyLevel loginStatusWriteConsistencyLevel;

    // RANKING (Final Scores)
    @Value("${cassandraRepository.hector.finalScoresReadConsistencyLevel}")
    private HConsistencyLevel finalScoresReadConsistencyLevel;
    @Value("${cassandraRepository.hector.finalScoresWriteConsistencyLevel}")
    private HConsistencyLevel finalScoresWriteConsistencyLevel;

    private Keyspace keyspace;
    private Keyspace rankingKeyspace;

    /**
     * Allows us to tune the consistency level per column family and per read or write operation
     */
    private ConfigurableConsistencyLevel consistencyLevelPolicy = new ConfigurableConsistencyLevel();
    private ConfigurableConsistencyLevel rankingConsistencyLevelPolicy = new ConfigurableConsistencyLevel();

    @PostConstruct
    void postConstruct() {
        if (isWarnEnabled) {
            logger.warn("CONNECTION INFO: cassandraRepository.hector.maxActiveConnections=" + maxActiveConnections);
        }

        if (cassandraHosts.indexOf(',') > 0) {
            cassandraHostsArray = SplitUtil.split(cassandraHosts, ',');
        } else {
            cassandraHostsArray = new String[] { cassandraHosts };
        }

        connect();
    }

    private void connect() {
        CassandraHostConfigurator hostConfiguration = new CassandraHostConfigurator();
        hostConfiguration.setHosts(cassandraHosts);
        hostConfiguration.setPort(cassandraPort);
        hostConfiguration.setMaxActive(maxActiveConnections);
        hostConfiguration.setExhaustedPolicy(ExhaustedPolicy.WHEN_EXHAUSTED_GROW);
        hostConfiguration.setAutoDiscoverHosts(autoDiscoverHosts);
        hostConfiguration.setAutoDiscoveryDelayInSeconds(autoDiscoveryDelayInSeconds);

        Cluster cluster = getOrCreateCluster(cassandraCluster, hostConfiguration);
        keyspace = createKeyspace(cassandraKeySpace, cluster);
        rankingKeyspace = createKeyspace(rankingKeyspaceName, cluster);

        if (!useDefaultConsistencyLevel) {
            // LOGIN
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(gameStatusReadConsistencyLevel, cfName(GameStatusMetadata.class), READ);
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(gameStatusWriteConsistencyLevel, cfName(GameStatusMetadata.class), WRITE);
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(gameStatusReadConsistencyLevel, cfName(GameStatusSuperColMetadata.class), READ);
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(gameStatusWriteConsistencyLevel, cfName(GameStatusSuperColMetadata.class), WRITE);

            // PARAMETER
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(parametersReadConsistencyLevel, cfName(ParametersMetadata.class), READ);
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(parametersWriteConsistencyLevel, cfName(ParametersMetadata.class), WRITE);

            // QUESTION
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(questionsReadConsistencyLevel, cfName(QuestionsMetadata.class), READ);
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(questionsWriteConsistencyLevel, cfName(QuestionsMetadata.class), WRITE);

            // SCORE
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(answersByMailReadConsistencyLevel, cfName(AnswersByMailMetadata.class), READ);
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(answersByMailWriteConsistencyLevel, cfName(AnswersByMailMetadata.class), WRITE);

            // USER
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(usersReadConsistencyLevel, cfName(UsersMetadata.class), READ);
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(usersWriteConsistencyLevel, cfName(UsersMetadata.class), WRITE);
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(usersReadConsistencyLevel, "UsersData", READ);
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(usersWriteConsistencyLevel, "UsersData", WRITE);

            // LOGIN STATUS
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(loginStatusReadConsistencyLevel, "DoubleLogin", READ);
            consistencyLevelPolicy.setConsistencyLevelForCfOperation(loginStatusWriteConsistencyLevel, "DoubleLogin", WRITE);

            keyspace.setConsistencyLevelPolicy(consistencyLevelPolicy);

            // Ranking
            rankingConsistencyLevelPolicy.setConsistencyLevelForCfOperation(finalScoresReadConsistencyLevel, cfName(FinalScoresMetadata.class), READ);
            rankingConsistencyLevelPolicy.setConsistencyLevelForCfOperation(finalScoresWriteConsistencyLevel, cfName(FinalScoresMetadata.class), WRITE);

            rankingKeyspace.setConsistencyLevelPolicy(rankingConsistencyLevelPolicy);

            // log it
            if (isWarnEnabled) {
                logger.warn("Using our consistency level for all CF operations...");
                logger.warn("cassandraRepository.hector.gameStatusReadConsistencyLevel={}", gameStatusReadConsistencyLevel);
                logger.warn("cassandraRepository.hector.gameStatusWriteConsistencyLevel={}", gameStatusWriteConsistencyLevel);

                logger.warn("cassandraRepository.hector.parametersReadConsistencyLevel={}", parametersReadConsistencyLevel);
                logger.warn("cassandraRepository.hector.parametersWriteConsistencyLevel={}", parametersWriteConsistencyLevel);

                logger.warn("cassandraRepository.hector.questionsReadConsistencyLevel={}", questionsReadConsistencyLevel);
                logger.warn("cassandraRepository.hector.questionsWriteConsistencyLevel={}", questionsWriteConsistencyLevel);

                logger.warn("cassandraRepository.hector.answersByMailReadConsistencyLevel={}", answersByMailReadConsistencyLevel);
                logger.warn("cassandraRepository.hector.answersByMailWriteConsistencyLevel={}", answersByMailWriteConsistencyLevel);

                logger.warn("cassandraRepository.hector.usersReadConsistencyLevel={}", usersReadConsistencyLevel);
                logger.warn("cassandraRepository.hector.usersWriteConsistencyLevel={}", usersWriteConsistencyLevel);

                // ranking
                logger.warn("cassandraRepository.hector.finalScoresReadConsistencyLevel={}", finalScoresReadConsistencyLevel);
                logger.warn("cassandraRepository.hector.finalScoresWriteConsistencyLevel={}", finalScoresWriteConsistencyLevel);
            }
        } else {
            if (isWarnEnabled) {
                logger.warn("ATTENTION: using default consistency level for all CF operations...");
            }
        }

        logger.warn("========= Start Hector Pools Status =========");
        List<String> poolsStatus = cluster.getConnectionManager().getStatusPerPool();
        for (String poolStatus : poolsStatus) {
            logger.warn(poolStatus);
        }
        logger.warn("========= End Hector Pools Status =========");
    }

    Keyspace keyspace() {
        return keyspace;
    }

    Keyspace rankingKeyspace() {
        return rankingKeyspace;
    }

    public void put(Class<?> columnFamily, Enum<?> key, long value) {
        put(columnFamily, key, "" + value);
    }

    public void put(Class<?> columnFamily, Enum<?> key, String value) {
        put(columnFamily, key.name(), value);
    }

    public void put(Class<?> columnFamily, String key, int value) {
        put(columnFamily, key, "" + value);
    }

    public void put(Class<?> columnFamily, String key, String value) {
        final Mutator<String> mutator = createMutator(keyspace(), stringSerializer);
        add(mutator, columnFamily, key, value);
        mutator.execute();
        if (isDebugEnabled) {
            logger.debug("Saved {} [{}][{}]={}", new Object[] { cfName(columnFamily), getDefaultKey(), key, value });
        }
    }

    public String getDefaultKey() {
        return VALUES;
    }

    @SuppressWarnings("rawtypes")
    public Mutator<String> add(Mutator<String> mutator, Class columnFamily, String key, String value) {
        value = StringUtils.trimToEmpty(value);
        mutator.addInsertion(getDefaultKey(), cfName(columnFamily), createStringColumn(key, value)); //
        if (isDebugEnabled) {
            logger.debug("Batched {} [{}][{}]={}", new Object[] { cfName(columnFamily), getDefaultKey(), key, value });
        }
        return mutator;
    }

    public String get(Class<?> columnFamily, Enum<?> key) {
        final HColumn<String, String> result = createColumnQuery(keyspace(), stringSerializer, stringSerializer, stringSerializer) //
                .setColumnFamily(cfName(columnFamily)) //
                .setKey(VALUES) //
                .setName(key.name()).execute() //
                .get();
        return result == null ? null : trimToNull(result.getValue());
    }

    // -------------------------------------
    // Fluent hector interface for our enum base keys
    // -------------------------------------

    public static String cfName(final Class<?> clazz) {
        String name = clazz.getSimpleName();
        if (name.endsWith("Metadata")) {
            return name.substring(0, name.length() - "Metadata".length());
        }
        return name;
    }

    public static HColumn<String, String> createStringColumn(Enum<?> key, String value) {
        return HFactory.createStringColumn(key.name(), value);
    }

    public static HColumn<String, String> createStringColumn(Enum<?> key, int value) {
        return HFactory.createStringColumn(key.name(), "" + value);
    }

    public static HColumn<String, String> createStringColumn(String key, int value) {
        return HFactory.createStringColumn(key, "" + value);
    }

    public static HColumn<String, String> createStringColumn(String key, String value) {
        return HFactory.createStringColumn(key, "" + value);
    }

    public static HColumn<String, String> createStringColumn(String key, StringBuilder value) {
        return HFactory.createStringColumn(key, value.toString());
    }

    public static HColumn<String, String> createStringColumn(String key, Enum<?> value) {
        return HFactory.createStringColumn(key, value.name());
    }

    public static HColumn<String, String> createStringColumn(String key, long value) {
        return HFactory.createStringColumn(key, "" + value);
    }

    public static HColumn<String, String> createStringColumn(String key, boolean value) {
        return HFactory.createStringColumn(key, "" + value);
    }

    public static HColumn<String, String> createStringColumn(Enum<?> key, long value) {
        return HFactory.createStringColumn(key.name(), "" + value);
    }

    // -------------------------------------
    // Recreating column families by recreating new ones with suffixes
    // -------------------------------------

    void truncate(final Class<?>... classes) {
        for (Class<?> clazz : classes) {
            truncate(clazz);
        }
    }

    void truncate(Class<?> clazz) {
        truncate(cfName(clazz));
    }

    void truncate(String columnFamily) {
        truncate(keyspace, columnFamily);
    }

    /**
     * Try JMX truncate on all hosts until it passes.
     */
    void truncate(Keyspace ks, String columnFamily) {
        Exception lastX = null;

        for (String host : cassandraHostsArray) {

            try {
                MBeanProxyFactoryBean factory = null;
                final String storageServiceObjectName = "org.apache.cassandra.db:type=StorageService";
                final String jmxUrl = "service:jmx:rmi:///jndi/rmi://" + host + ":" + cassandraJmxPort + "/jmxrmi";
                factory = new MBeanProxyFactoryBean();
                factory.setConnectOnStartup(false);
                factory.setRefreshOnConnectFailure(true);
                factory.setServiceUrl(jmxUrl);
                factory.setObjectName(storageServiceObjectName);
                factory.setProxyInterface(CassendraRemoteStorageServiceInterface.class);
                factory.afterPropertiesSet();
                CassendraRemoteStorageServiceInterface storage = (CassendraRemoteStorageServiceInterface) factory.getObject();
                storage.truncate(ks.getKeyspaceName(), columnFamily);
                logger.warn("Column family {}/{} truncated OK via {}/JMX", new Object[] { columnFamily, ks.getKeyspaceName(), host });
                return; // as soon as it is ok, we return
            } catch (Exception e) {
                logger.warn("Column family {}/{} truncate FAILED via {}/JMX", new Object[] { columnFamily, ks.getKeyspaceName(), host });
                logger.warn("here is the exception", e);
                lastX = e;
            }
        }

        throw new GameSessionException("Could not truncate CF " + columnFamily, lastX);
    }

    @ManagedAttribute
    public String getCassandraCluster() {
        return cassandraCluster;
    }

    @ManagedAttribute
    public void setCassandraCluster(String cassandraCluster) {
        this.cassandraCluster = cassandraCluster;
    }

    @ManagedAttribute
    public String getCassandraHosts() {
        return cassandraHosts;
    }

    @ManagedAttribute
    public void setCassandraHosts(String cassandraHosts) {
        this.cassandraHosts = cassandraHosts;
    }

    @ManagedAttribute
    public String getCassandraKeySpace() {
        return cassandraKeySpace;
    }

    @ManagedAttribute
    public void setCassandraKeySpace(String cassandraKeySpace) {
        this.cassandraKeySpace = cassandraKeySpace;
    }

    @ManagedAttribute
    public int getCassandraPort() {
        return cassandraPort;
    }

    @ManagedAttribute
    public void setCassandraPort(int cassandraPort) {
        this.cassandraPort = cassandraPort;
    }
}