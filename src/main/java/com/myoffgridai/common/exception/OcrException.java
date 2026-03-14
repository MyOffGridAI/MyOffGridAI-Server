package com.myoffgridai.common.exception;

/**
 * Thrown when OCR text extraction fails.
 *
 * <p>Typically maps to HTTP 500 Internal Server Error.</p>
 */
public class OcrException extends RuntimeException {

    public OcrException(String message) {
        super(message);
    }

    public OcrException(String message, Throwable cause) {
        super(message, cause);
    }
}
