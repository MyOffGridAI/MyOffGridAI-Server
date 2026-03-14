package com.myoffgridai.common.exception;

/**
 * Thrown when an attempt is made to create a resource that already exists.
 *
 * <p>Typically maps to HTTP 409 Conflict.</p>
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
