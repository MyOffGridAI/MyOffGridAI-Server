package com.myoffgridai.mcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a new MCP API token.
 *
 * @param name human-readable label for the token (e.g. "Claude Desktop")
 */
public record CreateMcpTokenRequest(
        @NotBlank(message = "Token name is required")
        @Size(max = 100, message = "Token name must not exceed 100 characters")
        String name
) {
}
