package com.myoffgridai.mcp.config;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link McpAuthentication}.
 */
class McpAuthenticationTest {

    @Test
    void constructor_setsAuthenticatedAndRole() {
        UUID ownerId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        McpAuthentication auth = new McpAuthentication(ownerId, tokenId);

        assertTrue(auth.isAuthenticated());
        assertEquals("mcpClient", auth.getPrincipal());
        assertEquals(ownerId, auth.getOwnerUserId());
        assertEquals(tokenId, auth.getTokenId());
        assertNull(auth.getCredentials());
    }

    @Test
    void authorities_containsMcpClientRole() {
        McpAuthentication auth = new McpAuthentication(UUID.randomUUID(), UUID.randomUUID());

        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_MCP_CLIENT",
                auth.getAuthorities().iterator().next().getAuthority());
    }
}
