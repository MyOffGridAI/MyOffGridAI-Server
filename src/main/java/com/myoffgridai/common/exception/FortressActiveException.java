package com.myoffgridai.common.exception;

/**
 * Thrown when an operation is blocked because fortress (lockdown) mode is active.
 *
 * <p>Fortress mode restricts all write operations to protect data integrity.
 * Typically maps to HTTP 403 Forbidden.</p>
 */
public class FortressActiveException extends RuntimeException {

    public FortressActiveException(String message) {
        super(message);
    }
}
