package usi2011.repository.cassandra;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static me.prettyprint.hector.api.factory.HFactory.createStringColumn;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.repository.UserRepository.UsersMetadata.EMAIL_JSON_QUOTED;
import static usi2011.repository.UserRepository.UsersMetadata.FIRSTNAME;
import static usi2011.repository.UserRepository.UsersMetadata.FIRSTNAME_JSON_QUOTED;
import static usi2011.repository.UserRepository.UsersMetadata.LASTNAME;
import static usi2011.repository.UserRepository.UsersMetadata.LASTNAME_JSON_QUOTED;
import static usi2011.repository.UserRepository.UsersMetadata.LOGGED_IN_GAME;
import static usi2011.repository.UserRepository.UsersMetadata.PASSWORD;
import static usi2011.repository.UserRepository.UsersMetadata.PASSWORD_JSON_QUOTED;
import static usi2011.repository.cassandra.CassandraRepository.cfName;
import static usi2011.util.LogUtil.isDebugEnabled;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.Profiles.HECTOR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.RangeSlicesQuery;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import usi2011.domain.User;
import usi2011.repository.UserRepository;
import usi2011.util.SplitUtil;

@Component
@ManagedResource
@Profile(HECTOR)
public class CassandraUserRepository implements UserRepository {
    private static final Logger logger = getLogger(CassandraUserRepository.class);
    private static final StringSerializer stringSerializer = StringSerializer.get();
    private static final BytesArraySerializer bytesArraySerializer = BytesArraySerializer.get();

    private static final String COLUMN_FAMILY = cfName(UsersMetadata.class);

    @Autowired
    private CassandraRepository cassandra;

    private AtomicLong userCreatedOKCounter = new AtomicLong(0);
    private AtomicLong userCreatedKOCounter = new AtomicLong(0);
    
    @Value("${cassandraUserRepository.nbUsersToLoadInMemory:0}")
    private int nbUsersToLoadInMemory;
    
    @Value("${cassandraUserRepository.enableUsersDataForCache}")
    private boolean enableUsersDataForCache;    
    
    private Map<String, User> userData; // cache


    public CassandraUserRepository() {
        if (isInfoEnabled) {
            new Timer(getClass().getSimpleName()).schedule(new TimerTask() {
                private long lastSum = 0;

                @Override
                public void run() {
                    long sum = userCreatedOKCounter.get() + userCreatedKOCounter.get();
                    if (sum != lastSum) {
                        if (isInfoEnabled) {
                            logger.info("created creation success={} failure={}", format("% 6d", userCreatedOKCounter.get()),
                                    format("% 6d", userCreatedKOCounter.get()));
                        }
                        lastSum = sum;
                    }
                }
            }, 0, SECONDS.toMillis(1));
        }
    }

    @ManagedOperation
    @Override
    public void reset() {
        if (isInfoEnabled) {
            logger.info("Reset users");
        }
        cassandra.truncate(COLUMN_FAMILY);
        userCreatedOKCounter.set(0);
        userCreatedKOCounter.set(0);
    }

    /**
     * Creates a single user, saves the precomputed json quoted elements if necessary
     * 
     * @return true if the user was created, false if it already exists! TODO: batch user insertion ?
     */
    @Override
    public boolean save(User user) {
        if (get(user.getEmail()) != null) {
            userCreatedKOCounter.getAndIncrement();
            if (isDebugEnabled) {
                logger.debug("{} batch create KO, nbOK={} nbKO={}", new Object[] { user.getEmail(), userCreatedOKCounter.get(), userCreatedKOCounter.get() });
            }
            return false;
        } else {
            Mutator<String> mutator = createMutator(cassandra.keyspace(), stringSerializer);
            mutator.addInsertion(user.getEmail(), COLUMN_FAMILY, createStringColumn(FIRSTNAME.name(), user.getFirstName())) //
                    .addInsertion(user.getEmail(), COLUMN_FAMILY, createStringColumn(LASTNAME.name(), user.getLastName())) //
                    .addInsertion(user.getEmail(), COLUMN_FAMILY, createStringColumn(PASSWORD.name(), user.getPassword())); //

            if (!user.getEmail().equals(user.getEmailJsonEscaped())) {
                mutator.addInsertion(user.getEmail(), COLUMN_FAMILY, createStringColumn(EMAIL_JSON_QUOTED.name(), user.getEmailJsonEscaped()));
            }
            if (!user.getPassword().equals(user.getPasswordJsonEscaped())) {
                mutator.addInsertion(user.getEmail(), COLUMN_FAMILY, createStringColumn(PASSWORD_JSON_QUOTED.name(), user.getPasswordJsonEscaped()));
            }
            if (!user.getLastName().equals(user.getLastNameJsonEscaped())) {
                mutator.addInsertion(user.getEmail(), COLUMN_FAMILY, createStringColumn(LASTNAME_JSON_QUOTED.name(), user.getLastNameJsonEscaped()));
            }
            if (!user.getFirstName().equals(user.getFirstNameJsonEscaped())) {
                mutator.addInsertion(user.getEmail(), COLUMN_FAMILY, createStringColumn(FIRSTNAME_JSON_QUOTED.name(), user.getFirstNameJsonEscaped()));
            }

            mutator.execute();
            
            saveInUsersData(user); // we save even if enableUsersDataForCache is false as we may need it afterwards
            
            userCreatedOKCounter.getAndIncrement();
            if (isDebugEnabled) {
                logger.debug("{} batch create OK, nbOK={} nbKO={}", new Object[] { user.getEmail(), userCreatedOKCounter, userCreatedKOCounter });
            }
            return true;
        }
    }
    

    @Override
    public void resetCacheHitStats() {
        counterCacheHit = 0;
        counterMissedCacheHit = 0;
    }
    public int counterCacheHit = 0;
    public int counterMissedCacheHit = 0;

    @Override
    public User get(String email) {
        if (userData != null) {
            User u = userData.get(email);
            if (u != null) {
                if (counterCacheHit++ > 0 && (counterCacheHit %200 == 0)) {
                    logger.warn("user cache hit: {}, user missed cache: {}", counterCacheHit, counterMissedCacheHit);
                }                
                return u;
            }
            
            counterMissedCacheHit++;
        }

        final ColumnSlice<String, String> result = createSliceQuery(cassandra.keyspace(), stringSerializer, stringSerializer, stringSerializer)
                .setColumnFamily(COLUMN_FAMILY) //
                .setKey(email) //
                .setColumnNames( //
                        EMAIL_JSON_QUOTED.name(), //
                        FIRSTNAME.name(), FIRSTNAME_JSON_QUOTED.name(), //
                        LASTNAME.name(), LASTNAME_JSON_QUOTED.name(), //
                        PASSWORD.name(), PASSWORD_JSON_QUOTED.name(), LOGGED_IN_GAME.name()) //
                .execute() //
                .get();
        if (result.getColumns().isEmpty()) {
            return null;
        }
        return new User(email //
                , result.getColumnByName(EMAIL_JSON_QUOTED.name()) == null ? null : result.getColumnByName(EMAIL_JSON_QUOTED.name()).getValue() //
                , result.getColumnByName(FIRSTNAME.name()) == null ? null : result.getColumnByName(FIRSTNAME.name()).getValue() //
                , result.getColumnByName(FIRSTNAME_JSON_QUOTED.name()) == null ? null : result.getColumnByName(FIRSTNAME_JSON_QUOTED.name()).getValue() //
                , result.getColumnByName(LASTNAME.name()) == null ? null : result.getColumnByName(LASTNAME.name()).getValue() //
                , result.getColumnByName(LASTNAME_JSON_QUOTED.name()) == null ? null : result.getColumnByName(LASTNAME_JSON_QUOTED.name()).getValue() //
                , result.getColumnByName(PASSWORD.name()) == null ? null : result.getColumnByName(PASSWORD.name()).getValue() //
                , result.getColumnByName(PASSWORD_JSON_QUOTED.name()) == null ? null : result.getColumnByName(PASSWORD_JSON_QUOTED.name()).getValue() //
                , result.getColumnByName(LOGGED_IN_GAME.name()) == null ? null : result.getColumnByName(LOGGED_IN_GAME.name()).getValue());
    }

    @Override
    public void userLoggedIn(User user, long gameId) {
        createMutator(cassandra.keyspace(), stringSerializer) //
                .addInsertion(user.getEmail(), COLUMN_FAMILY, createStringColumn(LOGGED_IN_GAME.name(), Long.toString(gameId))) //
                .execute();
    }

    @ManagedAttribute
    public long getUserCreatedKOCounter() {
        return userCreatedKOCounter.get();
    }

    @ManagedAttribute
    public void setUserCreatedKOCounter(long userCreatedKOCounter) {
        this.userCreatedKOCounter.set(userCreatedKOCounter);
    }

    @ManagedAttribute
    public long getUserCreatedOKCounter() {
        return userCreatedOKCounter.get();
    }

    @ManagedAttribute
    public void setUserCreatedOKCounter(long userCreatedOKCounter) {
        this.userCreatedOKCounter.set(userCreatedOKCounter);
    }

    @Override
    public void clearAllUsersFromMemory() {
        userData = null;
    }
    
    @Override
    public void loadUsersInMemory() {
        if (enableUsersDataForCache) { 
            loadUsersData();
        } else {
            loadUsersInMemoryLegacy();            
        }
    }
    
    // ================================================================================================================================
    
    public void loadUsersInMemoryLegacy() {
        if (nbUsersToLoadInMemory == 0) {
            logger.warn("cassandraUserRepository.nbUsersToLoadInMemory={}. ==> Skipping Loading users...", nbUsersToLoadInMemory);
            return;
        }
        
        logger.warn("Loading at most {} users in memory...", nbUsersToLoadInMemory);
        RangeSlicesQuery<String, String, String> rangeSlicesQuery = HFactory.createRangeSlicesQuery(cassandra.keyspace(), stringSerializer, stringSerializer,
                stringSerializer);

        OrderedRows<String, String, String> result = rangeSlicesQuery.setColumnFamily(COLUMN_FAMILY)
                .setColumnNames(FIRSTNAME.name(), LASTNAME.name(), PASSWORD.name()) //
                //.setRange("", "", false, count + 2) // ==> useless + limit is not used!
                .setRowCount(nbUsersToLoadInMemory)
                .execute().get();

        userData = new HashMap<String, User>(nbUsersToLoadInMemory);

        for (Row<String, String, String> ss : result) {
            ColumnSlice<String, String> cs = ss.getColumnSlice();
            //logger.warn("loading {}", ss.getKey());
            HColumn<String, String> firstname = cs.getColumnByName(FIRSTNAME.name());
            HColumn<String, String> lastname = cs.getColumnByName(LASTNAME.name());
            HColumn<String, String> password = cs.getColumnByName(PASSWORD.name());
            userData.put(ss.getKey(), new User(ss.getKey(), firstname.getValue(), lastname.getValue(), password.getValue()));
        }
        logger.warn("Users preloaded count: {}", userData.size());
    }
    
    
    // ================================================================================================================================
    
    private void saveInUsersData(User u) {
        Mutator<String> mutator = createMutator(cassandra.keyspace(), stringSerializer);
        StringBuilder userKey = new StringBuilder();
        userKey.append(u.getEmail()).append(":").append(u.getFirstName()).append(":").append(u.getLastName()).append(":").append(u.getPassword());
        
        mutator.addInsertion(
                userKey.toString(),
                "UsersData", 
                // kind of column less row...
                createColumn(new byte[]{"0".getBytes()[0]}, new byte[0], bytesArraySerializer, bytesArraySerializer));
        mutator.execute();
    }
    
    public void loadUsersData() {
        if (nbUsersToLoadInMemory == 0) {
            logger.warn("cassandraUserRepository.nbUsersToLoadInMemory={}. ==> Skipping Loading users...", nbUsersToLoadInMemory);
            return;
        }
        
        logger.warn("Loading at most {} UsersData in memory...", nbUsersToLoadInMemory);
        RangeSlicesQuery<String, byte[], byte[]> rangeSlicesQuery = HFactory.createRangeSlicesQuery(cassandra.keyspace(), stringSerializer, bytesArraySerializer,
                bytesArraySerializer);

        OrderedRows<String, byte[], byte[]> result = rangeSlicesQuery.setColumnFamily("UsersData")
                .setRowCount(nbUsersToLoadInMemory)
                .setReturnKeysOnly()                
                .execute().get();

        userData = new HashMap<String, User>(nbUsersToLoadInMemory);

        for (Row<String, byte[], byte[]> ss : result) {
            String [] user = SplitUtil.split(ss.getKey(), ':', 4);
            userData.put(user[0], new User(user[0], user[1], user[2], user[3]));
        }
        logger.warn("UsersData preloaded count: {}", userData.size());
    }
}
