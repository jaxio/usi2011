package usi2011.repository.cassandra;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static usi2011.repository.cassandra.HectorUtil.createStringBytesArrayColumn;
import static usi2011.util.Profiles.HECTOR;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import usi2011.repository.DoubleLoginRepository;

@Component
@ManagedResource
@Profile(HECTOR)
public class CassandraDoubleLoginRepository implements DoubleLoginRepository {
    private static final StringSerializer stringSerializer = StringSerializer.get();
    private static final BytesArraySerializer bytesArraySerializer = BytesArraySerializer.get();
    public static final String CF_DOUBLE_LOGIN = "DoubleLogin";
    private static final String ROW_KEY = "values";

    @Autowired
    private CassandraRepository cassandra;

    @Override
    @ManagedOperation
    public void reset() {
        cassandra.truncate("DoubleLogin");
    }

    @Override
    public void userLoggedIn(String email) {
        final Mutator<String> mutator = createMutator(cassandra.keyspace(), stringSerializer);
        mutator.addInsertion(ROW_KEY, CF_DOUBLE_LOGIN, createStringBytesArrayColumn(email, new byte[0]));
        mutator.execute();
    }

    @Override
    public boolean isUserLoggedIn(String email) {
        ColumnQuery<String, String, byte[]> cq = HFactory.createColumnQuery(cassandra.keyspace(), stringSerializer, stringSerializer, bytesArraySerializer);
        return cq.setColumnFamily(CF_DOUBLE_LOGIN).setKey(ROW_KEY).setName(email).execute().get() != null;
    }
}
