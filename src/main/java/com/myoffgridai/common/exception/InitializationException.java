package com.myoffgridai.common.exception;

/**
 * Thrown when a system initialization step fails or is attempted out of sequence.
 *
 * <p>Typically maps to HTTP 500 Internal Server Error or HTTP 409 Conflict
 * depending on context.</p>
 */
public class InitializationException extends RuntimeException {

    public InitializationException(String message) {
        super(message);
    }
}
