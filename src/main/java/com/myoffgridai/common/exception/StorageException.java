package com.myoffgridai.common.exception;

/**
 * Thrown when a file storage operation fails (write, read, delete).
 *
 * <p>Typically maps to HTTP 500 Internal Server Error.</p>
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
