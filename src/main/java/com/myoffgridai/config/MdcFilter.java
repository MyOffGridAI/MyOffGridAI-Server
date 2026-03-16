package com.myoffgridai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that populates the SLF4J Mapped Diagnostic Context (MDC)
 * with a unique request ID and authenticated user information.
 *
 * <p>Runs as the first filter in the chain ({@code @Order(2)}, after the
 * {@link CaptivePortalRedirectFilter} at {@code @Order(1)}) to ensure
 * every downstream log statement includes correlation fields:
 * {@code requestId}, {@code userId}, and {@code username}.</p>
 *
 * <p>The MDC is cleared in a {@code finally} block to prevent thread-local
 * leakage in servlet container thread pools.</p>
 */
@Component
@Order(2)
public class MdcFilter extends OncePerRequestFilter {

    /** MDC key for the unique request correlation ID. */
    static final String MDC_REQUEST_ID = "requestId";

    /** MDC key for the authenticated user's display name or principal. */
    static final String MDC_USERNAME = "username";

    /** MDC key for the authenticated user's principal (typically username). */
    static final String MDC_USER_ID = "userId";

    /**
     * Populates MDC with a unique request ID and, if available, the
     * authenticated user's identity. Clears MDC after the request completes.
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
        try {
            MDC.put(MDC_REQUEST_ID, UUID.randomUUID().toString());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                MDC.put(MDC_USERNAME, auth.getName());
                MDC.put(MDC_USER_ID, auth.getName());
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
