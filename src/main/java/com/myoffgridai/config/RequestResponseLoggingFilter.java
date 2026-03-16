package com.myoffgridai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that logs HTTP request and response metadata at DEBUG level.
 *
 * <p>Logs the HTTP method, request URI, response status code, and request
 * duration in milliseconds. This filter intentionally <strong>never</strong>
 * logs request/response bodies or authorization headers to protect sensitive
 * data.</p>
 *
 * <p>Requests to {@code /actuator} and {@code /setup} paths are excluded
 * to reduce noise from health checks and Wi-Fi setup polling.</p>
 *
 * <p>Runs at {@code @Order(4)}, after {@link MdcFilter} (so log lines carry
 * MDC correlation fields) and after {@link RateLimitingFilter} (so only
 * non-throttled requests are logged).</p>
 */
@Component
@Order(4)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    /**
     * Logs HTTP method, URI, status, and duration at DEBUG level.
     * Skips actuator and setup paths.
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
        String path = request.getRequestURI();

        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (log.isDebugEnabled()) {
                long duration = System.currentTimeMillis() - start;
                log.debug("{} {} — {} ({}ms)",
                        request.getMethod(), path, response.getStatus(), duration);
            }
        }
    }

    /**
     * Determines whether the given path should be excluded from logging.
     *
     * @param path the request URI
     * @return true if the path should be skipped
     */
    boolean shouldSkip(String path) {
        return path.startsWith("/actuator") || path.startsWith("/setup");
    }
}
