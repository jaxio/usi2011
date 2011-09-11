package usi2011.repository.cassandra;

import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.HColumn;

public final class HectorUtil {
    private static final BooleanSerializer booleanSerializer = BooleanSerializer.get();
    private static final StringSerializer stringSerializer = StringSerializer.get();
    private static final IntegerSerializer integerSerializer = IntegerSerializer.get();
    private static final LongSerializer longSerializer = LongSerializer.get();
    private static final BytesArraySerializer bytesArraySerializer = BytesArraySerializer.get();

    /**
     * Convenience method for creating a column with a String name and a Boolean value.
     */
    public static HColumn<String, Boolean> createStringBooleanColumn(String name, boolean value) {
        return createColumn(name, value, stringSerializer, booleanSerializer);
    }


    /**
     * Convenience method for creating a column with a String name and an Integer value.
     */
    public static HColumn<String, Integer> createStringIntegerColumn(String name, int value) {
        return createColumn(name, value, stringSerializer, integerSerializer);
    }

    /**
     * Convenience method for creating a column with a String name and a Long value.
     */
    public static HColumn<String, Long> createStringLongColumn(String name, long value) {
        return createColumn(name, value, stringSerializer, longSerializer);
    }

    /**
     * Convenience method for creating a column with an Integer name and an Integer value.
     */
    public static HColumn<Integer, Integer> createIntegerColumn(int name, int value) {
        return createColumn(name, value, integerSerializer, integerSerializer);
    }
    
    /**
     * Convenience method for creating a column with a String name and an byte[] value.
     */
    public static HColumn<String, byte[]> createStringBytesArrayColumn(String name, byte[] value) {
        return createColumn(name, value, stringSerializer, bytesArraySerializer);
    }    
}
