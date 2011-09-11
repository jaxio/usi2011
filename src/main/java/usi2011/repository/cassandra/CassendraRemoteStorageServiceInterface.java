package usi2011.repository.cassandra;

public interface CassendraRemoteStorageServiceInterface {
    void truncate(String keyspace, String columnFamily);
}
