package com.myoffgridai.common.exception;

/**
 * Thrown when a sensor serial port connection fails — port not found,
 * cannot be opened, or communication error during operation.
 */
public class SensorConnectionException extends RuntimeException {

    public SensorConnectionException(String message) {
        super(message);
    }

    public SensorConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
