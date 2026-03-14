package com.myoffgridai.common.exception;

/**
 * Thrown when the Ollama LLM service is unreachable or not responding.
 *
 * <p>Typically maps to HTTP 503 Service Unavailable.</p>
 */
public class OllamaUnavailableException extends RuntimeException {

    public OllamaUnavailableException(String message) {
        super(message);
    }

    public OllamaUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
