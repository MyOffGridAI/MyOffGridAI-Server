package com.myoffgridai.common.exception;

/**
 * Thrown when an uploaded file has an unsupported MIME type.
 *
 * <p>Typically maps to HTTP 400 Bad Request.</p>
 */
public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
