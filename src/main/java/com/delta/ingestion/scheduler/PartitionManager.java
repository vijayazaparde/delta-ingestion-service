package com.delta.ingestion.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test") // Prevents Postgres SQL from running during H2/Unit tests
public class PartitionManager {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0 23 * * *")
    public void createTomorrowPartition() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        String suffix = tomorrow.format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        String start = tomorrow.toString();
        String end = tomorrow.plusDays(1).toString();

        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS staging_customer_%s PARTITION OF staging_customer " +
                        "FOR VALUES FROM ('%s 00:00:00') TO ('%s 00:00:00')",
                suffix, start, end
        );

        log.info("Ensuring partition exists: staging_customer_{}", suffix);
        jdbcTemplate.execute(sql);
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void dropOldPartitions() {
        LocalDate oldDate = LocalDate.now().minusDays(2);
        String tableName = "staging_customer_" + oldDate.format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));

        log.info("Dropping old partition for instant cleanup: {}", tableName);
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName);
    }
}