package com.myoffgridai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.common.response.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Servlet filter that enforces per-IP rate limiting using the Bucket4j
 * token-bucket algorithm.
 *
 * <p>Two rate limit tiers are applied:</p>
 * <ul>
 *   <li><strong>Authentication endpoints</strong> ({@code /api/auth/**}):
 *       {@value AppConstants#RATE_LIMIT_AUTH_CAPACITY} requests per minute —
 *       protects against credential-stuffing attacks.</li>
 *   <li><strong>All other API endpoints</strong>:
 *       {@value AppConstants#RATE_LIMIT_API_CAPACITY} requests per minute —
 *       general abuse prevention.</li>
 * </ul>
 *
 * <p>Buckets are stored in a {@link ConcurrentHashMap} keyed by client IP.
 * This is appropriate for the single-node off-grid deployment model.
 * The filter can be disabled via the {@code app.rate-limiting.enabled} property.</p>
 *
 * <p>Runs at {@code @Order(3)}, after {@link MdcFilter} and before
 * {@link JwtAuthFilter}, so that rate-limited responses still carry
 * MDC correlation fields.</p>
 */
@Component
@Order(3)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final ConcurrentMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> apiBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    /**
     * Constructs the rate limiting filter.
     *
     * @param objectMapper the Jackson object mapper for JSON serialization
     * @param enabled      whether rate limiting is active (from config)
     */
    public RateLimitingFilter(ObjectMapper objectMapper,
                              @Value("${app.rate-limiting.enabled:true}") boolean enabled) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    /**
     * Checks the client's rate limit bucket for the requested endpoint tier.
     * If the bucket is exhausted, responds with HTTP 429 and an
     * {@link ApiResponse} error body. Otherwise, the request proceeds.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        Bucket bucket = resolveBucket(clientIp, path);
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error(AppConstants.RATE_LIMIT_EXCEEDED_MESSAGE));
        }
    }

    /**
     * Resolves the appropriate rate limit bucket for the given client IP
     * and request path. Auth endpoints get a stricter limit.
     *
     * @param clientIp the client's IP address
     * @param path     the request URI path
     * @return the Bucket4j bucket for this client/tier combination
     */
    Bucket resolveBucket(String clientIp, String path) {
        if (path.startsWith("/api/auth/")) {
            return authBuckets.computeIfAbsent(clientIp, k -> createBucket(
                    AppConstants.RATE_LIMIT_AUTH_CAPACITY));
        }
        return apiBuckets.computeIfAbsent(clientIp, k -> createBucket(
                AppConstants.RATE_LIMIT_API_CAPACITY));
    }

    /**
     * Creates a new token bucket with the given capacity that refills
     * fully every {@link AppConstants#RATE_LIMIT_REFILL_MINUTES} minutes.
     *
     * @param capacity the maximum number of tokens
     * @return a new Bucket4j bucket
     */
    private Bucket createBucket(int capacity) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity,
                                Duration.ofMinutes(AppConstants.RATE_LIMIT_REFILL_MINUTES))
                        .build())
                .build();
    }

    /**
     * Extracts the client IP address from the request, considering
     * the {@code X-Forwarded-For} header for reverse-proxy scenarios.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Returns whether rate limiting is enabled.
     *
     * @return true if rate limiting is active
     */
    public boolean isEnabled() {
        return enabled;
    }
}
