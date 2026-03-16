package com.myoffgridai.mcp.dto;

import java.util.UUID;

/**
 * Result of creating an MCP API token, returned exactly once to the user.
 *
 * <p>The {@code plaintextToken} is the actual token the MCP client will use.
 * It is never stored on the server (only its BCrypt hash is persisted).</p>
 *
 * @param tokenId        the persisted token entity ID
 * @param plaintextToken the raw token — shown ONCE, never retrievable again
 * @param name           the human-readable label for this token
 */
public record McpTokenCreateResult(
        UUID tokenId,
        String plaintextToken,
        String name
) {
}
