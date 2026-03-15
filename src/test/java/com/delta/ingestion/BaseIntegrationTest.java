package com.delta.ingestion;

import com.delta.ingestion.config.TestRedisConfig;
import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Common base class for Integration Tests.
 * Handles Testcontainers setup for Postgres and Redis.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public abstract class BaseIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        registry.add("spring.jpa.database-platform",
                () -> "org.hibernate.dialect.PostgreSQLDialect");

        registry.add("spring.jpa.hibernate.ddl-auto",
                () -> "create-drop");
    }

    @Autowired
    protected RedissonClient redissonClient;

    @Autowired
    protected Bucket ingestionBucket;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * Seed lookup data required by ingestion tests.
     */
    @BeforeEach
    void setupLookupData() {

        jdbcTemplate.execute(
                "INSERT INTO countries (code, name) VALUES ('IN', 'India') ON CONFLICT DO NOTHING");

        jdbcTemplate.execute(
                "INSERT INTO countries (code, name) VALUES ('US', 'United States') ON CONFLICT DO NOTHING");

        jdbcTemplate.execute("""
                INSERT INTO customer_status(code, name)
                VALUES
                    ('ACTIVE', 'Active'),
                    ('INACTIVE', 'Inactive')
                ON CONFLICT (code) DO NOTHING
                """);
    }

    /**
     * Helper to pre-insert a customer into DB
     * used by delta detection tests.
     */
    protected void setupExistingCustomerInDb(
            String externalId,
            String name,
            String countryCode,
            String statusCode) {

        Long countryId = jdbcTemplate.queryForObject(
                "SELECT id FROM countries WHERE code = ?",
                Long.class,
                countryCode.toUpperCase());

        Long statusId = jdbcTemplate.queryForObject(
                "SELECT id FROM customer_status WHERE code = ?",
                Long.class,
                statusCode.toUpperCase());

        String sql = """
                INSERT INTO customers
                    (external_id, name, email, country_id, status_id, created_at)
                VALUES (?, ?, ?, ?, ?, NOW())
                ON CONFLICT (external_id) DO NOTHING
                """;

        jdbcTemplate.update(
                sql,
                externalId,
                name,
                externalId.toLowerCase() + "@example.com",
                countryId,
                statusId
        );
    }

    /**
     * Optional helper if a test needs a clean DB.
     */
    protected void cleanupDatabase() {

        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    customers,
                    processed_requests,
                    staging_customer
                RESTART IDENTITY CASCADE
                """);
    }

    /**
     * Provide isolated Micrometer registry for tests
     * so metrics don't leak across tests.
     */
    @TestConfiguration
    static class TestMetricsConfig {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}