package com.myoffgridai.mcp.config;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.mcp.model.McpApiToken;
import com.myoffgridai.mcp.service.McpTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authentication filter for MCP SSE endpoints ({@code /mcp/**}).
 *
 * <p>Reads the {@code Authorization: Bearer {token}} header and validates
 * the token against stored BCrypt hashes via {@link McpTokenService}.
 * On success, sets an {@link McpAuthentication} in the SecurityContext.
 * On failure, returns HTTP 401.</p>
 *
 * <p>This filter only applies to {@code /mcp/**} paths. All other paths
 * are skipped via {@link #shouldNotFilter(HttpServletRequest)}.</p>
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public class McpAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(McpAuthFilter.class);

    private final McpTokenService mcpTokenService;

    /**
     * Constructs the filter with the MCP token service.
     *
     * @param mcpTokenService the token validation service
     */
    public McpAuthFilter(McpTokenService mcpTokenService) {
        this.mcpTokenService = mcpTokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/mcp/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(AppConstants.AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(AppConstants.BEARER_PREFIX)) {
            sendUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        String plaintext = authHeader.substring(AppConstants.BEARER_PREFIX.length());
        McpApiToken token = mcpTokenService.validateToken(plaintext);

        if (token == null) {
            sendUnauthorized(response, "Invalid MCP API token");
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(
                new McpAuthentication(token.getCreatedBy(), token.getId()));

        // Update last-used timestamp asynchronously (best-effort)
        try {
            mcpTokenService.updateLastUsed(token.getId());
        } catch (Exception e) {
            log.debug("Failed to update MCP token last-used timestamp: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"" + message + "\"}");
    }
}
