package usi2011.repository.cassandra;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.min;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.repository.GameStatusRepository.GameStatusMetadata.GAME_CREATION_TIME;
import static usi2011.repository.GameStatusRepository.GameStatusMetadata.NB_USER_LOGGED_FOR_MACHINE;
import static usi2011.repository.GameStatusRepository.GameStatusMetadata.QUESTION_1_TIMEFRAME_START;
import static usi2011.repository.GameStatusRepository.GameStatusMetadata.WHEN_FIRST_LOGGED_IN_MILLISECONDS;
import static usi2011.repository.cassandra.CassandraRepository.cfName;
import static usi2011.repository.cassandra.HectorUtil.createStringLongColumn;
import static usi2011.util.LogUtil.isDebugEnabled;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.Profiles.HECTOR;

import java.util.List;

import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HSuperColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SuperColumnQuery;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import usi2011.domain.GameStatus;
import usi2011.domain.GameStatus.GameStatusBuilder;
import usi2011.repository.GameStatusRepository;

@Component
@ManagedResource
@Profile(HECTOR)
public class CassandraGameStatusRepository implements GameStatusRepository {
    private static final Logger logger = getLogger(CassandraGameStatusRepository.class);
    private static final StringSerializer stringSerializer = StringSerializer.get();
    private static final LongSerializer longSerializer = LongSerializer.get();
    private static final IntegerSerializer intSerializer = IntegerSerializer.get();
    private static final String uniqueId = randomAlphabetic(15);
    private static String COLUMN_FAMILY = cfName(GameStatusMetadata.class);
    private static final int MANY_COLUMNS = 10000000;
    @Autowired
    private CassandraRepository cassandra;

    @Override
    @ManagedOperation
    public void reset() {
        cassandra.truncate(GameStatusMetadata.class, RankingStatusMetadata.class);
    }

    @Override
    @ManagedOperation
    public void loggedUsers(int nbLoggedUsers) {
        if (isDebugEnabled) {
            logger.debug("Saving loggedUsers {}", nbLoggedUsers);
        }
        final String row = cassandra.getDefaultKey();
        final String key = NB_USER_LOGGED_FOR_MACHINE + "-" + uniqueId;
        createMutator(cassandra.keyspace(), stringSerializer) //
                .addInsertion(row, COLUMN_FAMILY, createStringLongColumn(key, nbLoggedUsers)) //
                .execute();
    }

    @Override
    @ManagedOperation
    public void firstLogin(long firstLoginInMilliseconds) {
        if (isInfoEnabled) {
            logger.info("Saving first login at {}", firstLoginInMilliseconds);
        }
        final String row = cassandra.getDefaultKey();
        final String key = WHEN_FIRST_LOGGED_IN_MILLISECONDS + "-" + uniqueId;
        createMutator(cassandra.keyspace(), stringSerializer) //
                .addInsertion(row, COLUMN_FAMILY, createStringLongColumn(key, firstLoginInMilliseconds)) //
                .execute();
    }

    @Override
    @ManagedOperation
    public void gameCreated(long gameCreationTime) {
        if (isInfoEnabled) {
            logger.info("Saving game created at {}", gameCreationTime);
        }
        final String row = cassandra.getDefaultKey();
        final String key = GAME_CREATION_TIME.name();
        createMutator(cassandra.keyspace(), stringSerializer) //
                .addInsertion(row, COLUMN_FAMILY, createStringLongColumn(key, gameCreationTime)) //
                .execute();
    }

    @Override
    public void question1TimeframeSTart(long question1TimeframeSTart) {
        if (isInfoEnabled) {
            logger.info("Saving question1TimeframeSTart {}", question1TimeframeSTart);
        }
        final String row = cassandra.getDefaultKey();
        final String key = QUESTION_1_TIMEFRAME_START + "-" + uniqueId;
        createMutator(cassandra.keyspace(), stringSerializer) //
                .addInsertion(row, COLUMN_FAMILY, createStringLongColumn(key, question1TimeframeSTart)) //
                .execute();
    }
    
    @Override
    public void tweetSent() {
        logger.warn("not implemented in this implementation");
    }

    @Override
    public GameStatus getGameStatus() {
        final ColumnSlice<String, Long> result = createSliceQuery(cassandra.keyspace(), stringSerializer, stringSerializer, longSerializer) //
                .setColumnFamily(COLUMN_FAMILY) //
                .setKey(cassandra.getDefaultKey()) //
                .setRange(null, null, false, MANY_COLUMNS) //
                .execute() //
                .get();
        if (result.getColumns().isEmpty()) {
            return null;
        }
        GameStatus def = GameStatus.DEFAULT;
        long nbOfUsersLogged = def.getNbOfUsersLogged();
        long whenFirstLogged = def.getWhenFirstUserLoggedInMilliseconds();
        long question1TimeframeSTart = def.getQuestion1TimeframeSTart();
        long gameCreationTime = def.getGameCreationTime();

        for (HColumn<String, Long> column : result.getColumns()) {
            final String key = column.getName();
            if (key.startsWith(WHEN_FIRST_LOGGED_IN_MILLISECONDS.name())) {
                whenFirstLogged = min(column.getValue(), whenFirstLogged);
            } else if (key.startsWith(NB_USER_LOGGED_FOR_MACHINE.name())) {
                nbOfUsersLogged += column.getValue();
            } else if (key.startsWith(QUESTION_1_TIMEFRAME_START.name())) {
                question1TimeframeSTart = min(column.getValue(), question1TimeframeSTart);
            } else if (key.equals(GAME_CREATION_TIME.name())) {
                gameCreationTime = column.getValue();
            }
        }
        return new GameStatusBuilder() //
                .setWhenFirstUserLoggedInMilliseconds(whenFirstLogged) //
                .setGameCreationTime(gameCreationTime) //
                .setNbOfUsersLogged(nbOfUsersLogged) //
                .setQuestion1TimeframeSTart(question1TimeframeSTart) //
                .build();
    }

    @Override
    public void publishLocalRankingRequestCount(long gameCreationTimeMs, int value) {
        publishLocalRankingRequestCount(uniqueId, gameCreationTimeMs, value);
    }

    @Override
    public void publishLocalRankingRequestCount(String machineUid, long gameCreationTimeMs, int value) {
        // create our sub column to store our local counter
        List<HColumn<String, Integer>> cols = newArrayList();
        cols.add(HectorUtil.createStringIntegerColumn(machineUid, value));

        final Mutator<String> mutator = createMutator(cassandra.keyspace(), stringSerializer);

        mutator.addInsertion(cassandra.getDefaultKey(), // row key value (we use one row only)
                cfName(RankingStatusMetadata.class), // Super Column Family name
                HFactory.createSuperColumn(gameCreationTimeMs, // superCol name
                        cols, // cols in that super col
                        longSerializer, // serializer for super col name
                        stringSerializer, // serializer for col name
                        intSerializer)).// serializer for col value
                execute();
    }

    @Override
    public int getGlobalRankingRequestCount(long gameCreationTimeMs) {
        SuperColumnQuery<String, Long, String, Integer> scq = HFactory.createSuperColumnQuery(cassandra.keyspace(), stringSerializer, longSerializer,
                stringSerializer, intSerializer);

        QueryResult<HSuperColumn<Long, String, Integer>> result = scq.setKey(cassandra.getDefaultKey()). //
                setColumnFamily(cfName(RankingStatusMetadata.class)). //
                setSuperName(gameCreationTimeMs).execute();

        HSuperColumn<Long, String, Integer> superCol = result.get();
        if (superCol == null) {
            return 0;
        }
        List<HColumn<String, Integer>> localCounters = superCol.getColumns();

        int globalCounter = 0;
        if (localCounters != null) {
            for (HColumn<String, Integer> localCounter : localCounters) {
                globalCounter += localCounter.getValue();
            }

            return globalCounter;
        }
        return 0;
    }
}
