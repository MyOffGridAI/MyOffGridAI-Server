package com.myoffgridai.mcp.dto;

import com.myoffgridai.mcp.model.McpApiToken;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary view of an MCP API token. Never includes the plaintext or hash.
 *
 * @param id         the token ID
 * @param name       the human-readable label
 * @param isActive   whether the token is active
 * @param lastUsedAt when the token was last used (nullable)
 * @param createdAt  when the token was created
 */
public record McpTokenSummaryDto(
        UUID id,
        String name,
        boolean isActive,
        Instant lastUsedAt,
        Instant createdAt
) {

    /**
     * Converts an {@link McpApiToken} entity to a summary DTO.
     *
     * @param token the entity
     * @return the summary DTO
     */
    public static McpTokenSummaryDto from(McpApiToken token) {
        return new McpTokenSummaryDto(
                token.getId(),
                token.getName(),
                token.isActive(),
                token.getLastUsedAt(),
                token.getCreatedAt()
        );
    }
}
