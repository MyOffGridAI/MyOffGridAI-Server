package com.myoffgridai.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Generic API response wrapper providing a consistent envelope for all endpoints.
 *
 * <p>Every controller endpoint returns its payload wrapped in this structure,
 * ensuring clients always receive a predictable shape containing success status,
 * an optional message, the data payload, a timestamp, and a request ID for tracing.</p>
 *
 * @param <T> the type of the data payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Instant timestamp;
    private String requestId;

    // Pagination fields (only present on paginated responses)
    private Long totalElements;
    private Integer page;
    private Integer size;

    public ApiResponse() {
        this.timestamp = Instant.now();
        this.requestId = UUID.randomUUID().toString();
    }

    /**
     * Creates a successful response with data and no message.
     *
     * @param data the response payload
     * @param <T>  the payload type
     * @return a successful {@link ApiResponse} containing the data
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        return response;
    }

    /**
     * Creates a successful response with data and a descriptive message.
     *
     * @param data    the response payload
     * @param message a human-readable success message
     * @param <T>     the payload type
     * @return a successful {@link ApiResponse} containing the data and message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.message = message;
        return response;
    }

    /**
     * Creates an error response with a descriptive message and no data.
     *
     * @param message a human-readable error message
     * @param <T>     the payload type (always null for errors)
     * @return an error {@link ApiResponse}
     */
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        return response;
    }

    /**
     * Creates a successful paginated response with pagination metadata.
     *
     * @param data  the page of results
     * @param total total number of elements across all pages
     * @param page  the current page number (zero-based)
     * @param size  the page size
     * @param <T>   the payload type
     * @return a paginated {@link ApiResponse}
     */
    public static <T> ApiResponse<T> paginated(T data, long total, int page, int size) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.totalElements = total;
        response.page = page;
        response.size = size;
        return response;
    }

    // ── Getters and Setters ─────────────────────────────────────────────────

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
