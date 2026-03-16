package com.myoffgridai.mcp.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

/**
 * Authentication token for MCP API clients.
 *
 * <p>Set in the SecurityContext by {@link McpAuthFilter} after validating
 * a Bearer token against stored BCrypt hashes. The principal is "mcpClient"
 * and the token's {@code createdBy} user ID is available for user-scoped queries.</p>
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public class McpAuthentication extends AbstractAuthenticationToken {

    private final String principal;
    private final UUID ownerUserId;
    private final UUID tokenId;

    /**
     * Creates an authenticated MCP token.
     *
     * @param ownerUserId the OWNER user who created the MCP token
     * @param tokenId     the MCP token entity ID
     */
    public McpAuthentication(UUID ownerUserId, UUID tokenId) {
        super(List.of(new SimpleGrantedAuthority("ROLE_MCP_CLIENT")));
        this.principal = "mcpClient";
        this.ownerUserId = ownerUserId;
        this.tokenId = tokenId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    /**
     * Returns the user ID of the OWNER who created this MCP token.
     *
     * @return the owner user ID for user-scoped queries
     */
    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    /**
     * Returns the MCP token entity ID.
     *
     * @return the token ID
     */
    public UUID getTokenId() {
        return tokenId;
    }
}
