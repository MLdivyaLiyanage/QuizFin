package com.perisic.fish.engine;

/** Storage failure exposed without coupling callers to JDBC exceptions. */
public final class ScoreStorageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ScoreStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
