package com.myoffgridai.common.exception;

/**
 * Thrown when Ollama returns an error during model inference.
 *
 * <p>Typically maps to HTTP 502 Bad Gateway.</p>
 */
public class OllamaInferenceException extends RuntimeException {

    public OllamaInferenceException(String message) {
        super(message);
    }

    public OllamaInferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
