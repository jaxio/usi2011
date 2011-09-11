package usi2011.repository.cassandra;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.repository.LoginStatusRepository.LoginStatusMetadata.IS_LOGGED;
import static usi2011.repository.cassandra.CassandraRepository.cfName;
import static usi2011.repository.cassandra.HectorUtil.createStringBooleanColumn;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.Profiles.HECTOR;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.ColumnSlice;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import usi2011.domain.User;
import usi2011.repository.LoginStatusRepository;

@Component
@ManagedResource
@Profile(HECTOR)
public class CassandraLoginStatusRepository implements LoginStatusRepository {
    private static final Logger logger = getLogger(CassandraLoginStatusRepository.class);
    private static final StringSerializer stringSerializer = StringSerializer.get();
    private static final BooleanSerializer booleanSerializer = BooleanSerializer.get();
    private static final String COLUMN_FAMILY = cfName(LoginStatusRepository.LoginStatusMetadata.class);

    @Autowired
    private CassandraRepository cassandra;
    @Value("${enforce.login.uniqueness}")
    private boolean enforceLoginUniqueness;

    private AtomicLong userNbLoggedIn = new AtomicLong(0);

    public CassandraLoginStatusRepository() {
        if (enforceLoginUniqueness && isInfoEnabled) {
            new Timer(getClass().getSimpleName()).schedule(new TimerTask() {
                private long lastValue = 0;

                @Override
                public void run() {
                    long value = userNbLoggedIn.get();
                    if (value != lastValue) {
                        logger.info("login set {}", format("% 6d", userNbLoggedIn.get()));
                        lastValue = value;
                    }
                }
            }, 0, SECONDS.toMillis(1));
        }
    }

    @ManagedOperation
    @Override
    public void reset() {
        if (!enforceLoginUniqueness) {
            return;
        }
        if (isInfoEnabled) {
            logger.info("Reset login status");
        }
        cassandra.truncate(COLUMN_FAMILY);
        userNbLoggedIn.set(0);
    }

    @Override
    public boolean isLoggedIn(final User user) {
        if (!enforceLoginUniqueness) {
            return false;
        }
        final ColumnSlice<String, Boolean> result = createSliceQuery(cassandra.keyspace(), stringSerializer, stringSerializer, booleanSerializer) //
                .setColumnFamily(COLUMN_FAMILY) //
                .setKey(user.getEmail()) //
                .setColumnNames(IS_LOGGED.name()) //
                .execute() //
                .get();
        return result.getColumns().isEmpty() ? false : result.getColumnByName(IS_LOGGED.name()).getValue();
    }

    @Override
    public void userLoggedIn(final User user) {
        if (!enforceLoginUniqueness) {
            return;
        }
        checkNotNull(user, "missing user");
        checkNotNull(user.getEmail(), "email is invalid");
        createMutator(cassandra.keyspace(), stringSerializer) //
                .addInsertion(user.getEmail(), COLUMN_FAMILY, createStringBooleanColumn(IS_LOGGED.name(), TRUE)) //
                .execute();
    }

    @ManagedAttribute
    public AtomicLong getUserNbLoggedIn() {
        return userNbLoggedIn;
    }

    @ManagedAttribute
    public void setUserNbLoggedIn(AtomicLong userNbLoggedIn) {
        this.userNbLoggedIn = userNbLoggedIn;
    }
}
