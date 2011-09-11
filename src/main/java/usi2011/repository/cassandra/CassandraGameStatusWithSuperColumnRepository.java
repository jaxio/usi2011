package usi2011.repository.cassandra;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.min;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.createSuperSliceQuery;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.repository.GameStatusRepository.GameStatusSuperColMetadata.GAME_CREATION_TIME;
import static usi2011.repository.GameStatusRepository.GameStatusSuperColMetadata.NB_TWEET_SENT_FOR_MACHINE;
import static usi2011.repository.GameStatusRepository.GameStatusSuperColMetadata.NB_USER_LOGGED_FOR_MACHINE;
import static usi2011.repository.GameStatusRepository.GameStatusSuperColMetadata.QUESTION_1_TIMEFRAME_START;
import static usi2011.repository.GameStatusRepository.GameStatusSuperColMetadata.WHEN_FIRST_LOGGED_IN_MILLISECONDS;
import static usi2011.repository.cassandra.CassandraRepository.cfName;
import static usi2011.util.LogUtil.isDebugEnabled;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.Profiles.HECTOR;

import java.net.InetAddress;
import java.util.List;

import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HSuperColumn;
import me.prettyprint.hector.api.beans.SuperSlice;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SuperColumnQuery;
import me.prettyprint.hector.api.query.SuperSliceQuery;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
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
@Primary
public class CassandraGameStatusWithSuperColumnRepository implements GameStatusRepository {
    private static final Logger logger = getLogger(CassandraGameStatusWithSuperColumnRepository.class);
    private static final StringSerializer stringSerializer = StringSerializer.get();
    private static final LongSerializer longSerializer = LongSerializer.get();
    private static final IntegerSerializer intSerializer = IntegerSerializer.get();
    private static final String uniqueId;
    private static String SUPER_COLUMN_FAMILY = cfName(GameStatusSuperColMetadata.class);
    @Autowired
    private CassandraRepository cassandra;

    private Long currentGameCreationTime = null;

    static {
        try { 
            uniqueId = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    @ManagedOperation
    public void reset() {
        // no need to truncate anything
        // no need to reset the currentGameCreationTime
    }

    @Override
    @ManagedOperation
    public void loggedUsers(int nbLoggedUsers) {
        if (currentGameCreationTime == null) {
            throw new IllegalStateException("Expecting a game to be present...");
        }

        if (isDebugEnabled) {
            logger.debug("Saving loggedUsers {}", nbLoggedUsers);
        }

        // create our sub column to store first login info
        final String key = NB_USER_LOGGED_FOR_MACHINE + "-" + uniqueId;
        List<HColumn<String, Long>> cols = newArrayList();
        cols.add(HectorUtil.createStringLongColumn(key, nbLoggedUsers));

        final Mutator<String> mutator = createMutator(cassandra.keyspace(), stringSerializer);
        mutator.addInsertion(cassandra.getDefaultKey(), // row key value (we use one row only)
                SUPER_COLUMN_FAMILY, // Super Column Family name
                HFactory.createSuperColumn(currentGameCreationTime, // superCol name
                        cols, // cols in that super col
                        longSerializer, // serializer for super col name
                        stringSerializer, // serializer for col name
                        longSerializer)).// serializer for col value
                execute();
    }

    @Override
    @ManagedOperation
    public void firstLogin(long firstLoginInMilliseconds) {
        if (currentGameCreationTime == null) {
            throw new IllegalStateException("Expecting a game to be present...");
        }

        if (isInfoEnabled) {
            logger.info("Saving first login at {}", firstLoginInMilliseconds);
        }

        // create our sub column to store first login info
        final String key = WHEN_FIRST_LOGGED_IN_MILLISECONDS + "-" + uniqueId;
        List<HColumn<String, Long>> cols = newArrayList();
        cols.add(HectorUtil.createStringLongColumn(key, firstLoginInMilliseconds));

        final Mutator<String> mutator = createMutator(cassandra.keyspace(), stringSerializer);

        mutator.addInsertion(cassandra.getDefaultKey(), // row key value (we use one row only)
                SUPER_COLUMN_FAMILY, // Super Column Family name
                HFactory.createSuperColumn(currentGameCreationTime, // superCol name
                        cols, // cols in that super col
                        longSerializer, // serializer for super col name
                        stringSerializer, // serializer for col name
                        longSerializer)).// serializer for col value
                execute();
    }

    @Override
    @ManagedOperation
    public void gameCreated(long gameCreationTime) {
        if (isInfoEnabled) {
            logger.info("Saving game created at {}", gameCreationTime);
        }
        // create our sub column to store our game info
        // we add one redundant col (the gameCreationTime) to please hector.
        List<HColumn<String, Long>> cols = newArrayList();
        cols.add(HectorUtil.createStringLongColumn(GAME_CREATION_TIME.name(), gameCreationTime));

        final Mutator<String> mutator = createMutator(cassandra.keyspace(), stringSerializer);

        mutator.addInsertion(cassandra.getDefaultKey(), // row key value (we use one row only)
                SUPER_COLUMN_FAMILY, // Super Column Family name
                HFactory.createSuperColumn(gameCreationTime, // superCol name
                        cols, // cols in that super col
                        longSerializer, // serializer for super col name
                        stringSerializer, // serializer for col name
                        longSerializer)).// serializer for col value
                execute();
        currentGameCreationTime = gameCreationTime;
    }

    @Override
    public void question1TimeframeSTart(long question1TimeframeSTart) {
        if (currentGameCreationTime == null) {
            throw new IllegalStateException("Expecting a game to be present...");
        }

        if (isInfoEnabled) {
            logger.info("Saving question1TimeframeSTart {}", question1TimeframeSTart);
        }

        // create our sub column to store first login info
        final String key = QUESTION_1_TIMEFRAME_START + "-" + uniqueId;
        List<HColumn<String, Long>> cols = newArrayList();
        cols.add(HectorUtil.createStringLongColumn(key, question1TimeframeSTart));

        final Mutator<String> mutator = createMutator(cassandra.keyspace(), stringSerializer);

        mutator.addInsertion(cassandra.getDefaultKey(), // row key value (we use one row only)
                SUPER_COLUMN_FAMILY, // Super Column Family name
                HFactory.createSuperColumn(currentGameCreationTime, // superCol name
                        cols, // cols in that super col
                        longSerializer, // serializer for super col name
                        stringSerializer, // serializer for col name
                        longSerializer)).// serializer for col value
                execute();

    }

    @Override
    public void tweetSent() {
        if (currentGameCreationTime == null) {
            throw new IllegalStateException("Expecting a game to be present...");
        }

        if (isInfoEnabled) {
            logger.info("Saving 1 tweet sent for game {}", currentGameCreationTime);
        }

        // create our sub column to store first login info
        final String key = NB_TWEET_SENT_FOR_MACHINE + "-" + uniqueId;
        List<HColumn<String, Long>> cols = newArrayList();
        cols.add(HectorUtil.createStringLongColumn(key, 1)); // 1 tweet sent by this machine

        final Mutator<String> mutator = createMutator(cassandra.keyspace(), stringSerializer);

        mutator.addInsertion(cassandra.getDefaultKey(), // row key value (we use one row only)
                SUPER_COLUMN_FAMILY, // Super Column Family name
                HFactory.createSuperColumn(currentGameCreationTime, // superCol name
                        cols, // cols in that super col
                        longSerializer, // serializer for super col name
                        stringSerializer, // serializer for col name
                        longSerializer)).// serializer for col value
                execute();

    }    
    
    @Override
    public GameStatus getGameStatus() {
        final SuperSliceQuery<String, Long, String, Long> ssq = createSuperSliceQuery(cassandra.keyspace(), stringSerializer, longSerializer, 
                stringSerializer, longSerializer);
            

        SuperSlice<Long,String,Long> result = ssq.setColumnFamily(SUPER_COLUMN_FAMILY) //
                .setKey(cassandra.getDefaultKey()) //
                .setRange(null, null, true, 1) // true= > we want the most recent game
                .execute() //
                .get();
        if (result.getSuperColumns().isEmpty()) {
            return null;
        }
        
        HSuperColumn<Long, String, Long> gameSuperCol = result.getSuperColumns().get(0);
        GameStatus def = GameStatus.DEFAULT;
        long nbOfUsersLogged = def.getNbOfUsersLogged();
        long whenFirstLogged = def.getWhenFirstUserLoggedInMilliseconds();
        long question1TimeframeSTart = def.getQuestion1TimeframeSTart();
        long gameCreationTime = def.getGameCreationTime();
        int nbTweetSent = def.getNbTweetSent();
        

        for (HColumn<String, Long> column : gameSuperCol.getColumns()) {
            final String key = column.getName();
            if (key.startsWith(WHEN_FIRST_LOGGED_IN_MILLISECONDS.name())) {
                whenFirstLogged = min(column.getValue(), whenFirstLogged);
            } else if (key.startsWith(NB_USER_LOGGED_FOR_MACHINE.name())) {
                nbOfUsersLogged += column.getValue();
            } else if (key.startsWith(NB_TWEET_SENT_FOR_MACHINE.name())) {
                nbTweetSent += column.getValue();
            } else if (key.startsWith(QUESTION_1_TIMEFRAME_START.name())) {
                question1TimeframeSTart = min(column.getValue(), question1TimeframeSTart);
            } else if (key.equals(GAME_CREATION_TIME.name())) {
                gameCreationTime = column.getValue();
            }
        }
        
        currentGameCreationTime = gameCreationTime;
        return new GameStatusBuilder() //
                .setWhenFirstUserLoggedInMilliseconds(whenFirstLogged) //
                .setGameCreationTime(gameCreationTime) //
                .setNbOfUsersLogged(nbOfUsersLogged) //
                .setQuestion1TimeframeSTart(question1TimeframeSTart) //
                .setNbTweetSent(nbTweetSent) //
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
