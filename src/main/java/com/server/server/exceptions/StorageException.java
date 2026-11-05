package com.server.server.exceptions;

/** A file could not be written or read on the server's disk (HTTP 500, {@code STORAGE_FAILED}). */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
