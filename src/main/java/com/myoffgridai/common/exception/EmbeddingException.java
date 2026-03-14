package com.myoffgridai.common.exception;

/**
 * Thrown when embedding generation fails due to Ollama unavailability
 * or other embedding pipeline errors.
 *
 * <p>Typically maps to HTTP 503 Service Unavailable.</p>
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
