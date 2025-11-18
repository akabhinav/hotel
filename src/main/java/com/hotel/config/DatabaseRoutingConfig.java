package com.hotel.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Database routing configuration for read/write separation and sharding.
 * Routes queries to appropriate data sources based on:
 * 1. Transaction type (read-only vs read-write)
 * 2. Shard key (hotel_id for sharding)
 */
public class DatabaseRoutingConfig extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> currentDataSource = new ThreadLocal<>();
    private static final ThreadLocal<Long> currentShardKey = new ThreadLocal<>();

    public enum DataSourceType {
        PRIMARY,
        REPLICA,
        SHARD_0,
        SHARD_1,
        SHARD_2,
        SHARD_3
        // Add more shards as needed
    }

    @Override
    protected Object determineCurrentLookupKey() {
        // Check if a specific shard is requested
        Long shardKey = currentShardKey.get();
        if (shardKey != null) {
            int shardNumber = calculateShard(shardKey);
            return DataSourceType.valueOf("SHARD_" + shardNumber);
        }

        // Check if a specific data source is set
        String dataSource = currentDataSource.get();
        if (dataSource != null) {
            return DataSourceType.valueOf(dataSource);
        }

        // Default routing based on transaction type
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        return isReadOnly ? DataSourceType.REPLICA : DataSourceType.PRIMARY;
    }

    /**
     * Calculate shard number based on hotel_id
     */
    public static int calculateShard(Long hotelId) {
        // Simple modulo-based sharding
        // In production, use consistent hashing for better distribution
        int numShards = 4;  // Configure based on actual number of shards
        return (int) (hotelId % numShards);
    }

    /**
     * Set the current data source for this thread
     */
    public static void setDataSource(DataSourceType dataSourceType) {
        currentDataSource.set(dataSourceType.name());
    }

    /**
     * Set the shard key for routing to appropriate shard
     */
    public static void setShardKey(Long hotelId) {
        currentShardKey.set(hotelId);
    }

    /**
     * Clear the routing context
     */
    public static void clearContext() {
        currentDataSource.remove();
        currentShardKey.remove();
    }

    /**
     * Force routing to primary database
     */
    public static void routeToPrimary() {
        setDataSource(DataSourceType.PRIMARY);
    }

    /**
     * Force routing to replica database
     */
    public static void routeToReplica() {
        setDataSource(DataSourceType.REPLICA);
    }
}
