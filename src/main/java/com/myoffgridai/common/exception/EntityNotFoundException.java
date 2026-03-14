package com.myoffgridai.common.exception;

/**
 * Thrown when a requested entity cannot be found in the data store.
 *
 * <p>Typically maps to HTTP 404 Not Found.</p>
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
