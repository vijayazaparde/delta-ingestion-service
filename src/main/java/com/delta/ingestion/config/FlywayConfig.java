package com.delta.ingestion.config;

import org.flywaydb.core.Flyway;

public class FlywayConfig {

    public static void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        "jdbc:postgresql://localhost:5432/assignment_db",
                        "postgres",
                        "admin"
                )
                .load();

        flyway.migrate();
    }
}