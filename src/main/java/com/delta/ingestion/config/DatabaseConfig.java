package com.delta.ingestion.config;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConfig {

    private static final String URL = "jdbc:postgresql://localhost:5432/assignment_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "admin";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException("DB Connection Failed", e);
        }
    }
}