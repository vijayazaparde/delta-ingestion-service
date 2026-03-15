package com.delta.ingestion.exception;

/**
 * Standard wrapper for database and persistence-level errors
 * occurring during the ingestion lifecycle.
 */
public class DatabaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}