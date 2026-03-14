package com.myoffgridai.common.exception;

/**
 * Thrown when a Privacy Fortress Mode operation fails (e.g., iptables command error).
 */
public class FortressOperationException extends RuntimeException {

    public FortressOperationException(String message) {
        super(message);
    }

    public FortressOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
