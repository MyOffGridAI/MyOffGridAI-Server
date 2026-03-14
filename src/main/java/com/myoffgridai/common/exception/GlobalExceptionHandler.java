package com.myoffgridai.common.exception;

import com.myoffgridai.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler that translates exceptions into consistent
 * {@link ApiResponse} error envelopes with appropriate HTTP status codes.
 *
 * <p>All exceptions thrown by controllers or services are intercepted here,
 * logged at the appropriate level, and returned to the client in a
 * predictable format. Sensitive internal details are never exposed.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles bean validation failures from {@code @Valid} annotated request bodies.
     *
     * @param ex the validation exception containing field-level errors
     * @return 400 Bad Request with field error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed: " + errors));
    }

    /**
     * Handles username-not-found lookups.
     *
     * @param ex the exception
     * @return 404 Not Found
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
        log.warn("Username not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles bad credentials during authentication.
     *
     * @param ex the exception
     * @return 401 Unauthorized
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed: bad credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid username or password"));
    }

    /**
     * Handles access denied (insufficient permissions).
     *
     * @param ex the exception
     * @return 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied"));
    }

    /**
     * Handles entity not found.
     *
     * @param ex the exception
     * @return 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles duplicate resource creation attempts.
     *
     * @param ex the exception
     * @return 409 Conflict
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicate(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles illegal argument errors.
     *
     * @param ex the exception
     * @return 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles fortress mode violations.
     *
     * @param ex the exception
     * @return 403 Forbidden
     */
    @ExceptionHandler(FortressActiveException.class)
    public ResponseEntity<ApiResponse<Object>> handleFortressActive(FortressActiveException ex) {
        log.warn("Fortress mode active: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles Ollama service unavailable.
     *
     * @param ex the exception
     * @return 503 Service Unavailable
     */
    @ExceptionHandler(OllamaUnavailableException.class)
    public ResponseEntity<ApiResponse<Object>> handleOllamaUnavailable(OllamaUnavailableException ex) {
        log.warn("Ollama unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles Ollama inference errors.
     *
     * @param ex the exception
     * @return 502 Bad Gateway
     */
    @ExceptionHandler(OllamaInferenceException.class)
    public ResponseEntity<ApiResponse<Object>> handleOllamaInference(OllamaInferenceException ex) {
        log.error("Ollama inference error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles embedding generation failures.
     *
     * @param ex the exception
     * @return 503 Service Unavailable
     */
    @ExceptionHandler(EmbeddingException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmbeddingException(EmbeddingException ex) {
        log.warn("Embedding error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles file storage failures.
     *
     * @param ex the exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResponse<Object>> handleStorageException(StorageException ex) {
        log.error("Storage error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles unsupported file type uploads.
     *
     * @param ex the exception
     * @return 400 Bad Request
     */
    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnsupportedFileType(UnsupportedFileTypeException ex) {
        log.warn("Unsupported file type: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles OCR processing failures.
     *
     * @param ex the exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(OcrException.class)
    public ResponseEntity<ApiResponse<Object>> handleOcrException(OcrException ex) {
        log.error("OCR error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles attempts to execute a disabled skill.
     *
     * @param ex the exception
     * @return 400 Bad Request
     */
    @ExceptionHandler(SkillDisabledException.class)
    public ResponseEntity<ApiResponse<Object>> handleSkillDisabled(SkillDisabledException ex) {
        log.warn("Skill disabled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Catch-all handler for unexpected exceptions. Logs the full stack trace
     * but returns only a generic message to the client.
     *
     * @param ex the exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}
