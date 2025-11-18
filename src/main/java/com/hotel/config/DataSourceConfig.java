package com.hotel.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Data source configuration with connection pooling optimization.
 * Supports:
 * - Primary/Replica routing for read/write separation
 * - Multiple shards for horizontal scaling
 * - HikariCP connection pool optimization
 */
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url:jdbc:h2:mem:hoteldb}")
    private String primaryUrl;

    @Value("${spring.datasource.username:sa}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    /**
     * Primary data source with optimized HikariCP configuration
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        return createHikariDataSource(primaryUrl, "HotelDB-Primary");
    }

    /**
     * Create HikariCP data source with optimized settings
     */
    private HikariDataSource createHikariDataSource(String jdbcUrl, String poolName) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName(poolName);

        // Pool size configuration
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);

        // Timeout configuration
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);

        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        // Validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        // Leak detection for debugging
        config.setLeakDetectionThreshold(60000);

        return new HikariDataSource(config);
    }

    /**
     * Routing data source for read/write separation (optional)
     * Enable with: spring.datasource.routing.enabled=true
     */
    @Bean
    @ConditionalOnProperty(name = "spring.datasource.routing.enabled", havingValue = "true")
    public DataSource routingDataSource() {
        DatabaseRoutingConfig routingDataSource = new DatabaseRoutingConfig();

        DataSource primaryDataSource = createHikariDataSource(primaryUrl, "HotelDB-Primary");

        // In production, configure replica URLs from properties
        // DataSource replicaDataSource = createHikariDataSource(replicaUrl, "HotelDB-Replica");

        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(DatabaseRoutingConfig.DataSourceType.PRIMARY, primaryDataSource);
        // dataSourceMap.put(DatabaseRoutingConfig.DataSourceType.REPLICA, replicaDataSource);

        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);

        return routingDataSource;
    }
}
