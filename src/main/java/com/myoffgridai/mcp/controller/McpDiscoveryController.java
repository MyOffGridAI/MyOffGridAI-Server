package com.myoffgridai.mcp.controller;

import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Discovery endpoint that returns MCP client configuration snippets
 * for Claude Desktop, Claude Code, and other MCP clients.
 *
 * <p>Returns the JSON configuration block that users can paste into
 * their MCP client's settings file (e.g., {@code claude_desktop_config.json}).</p>
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@RestController
@RequestMapping(AppConstants.API_MCP)
@PreAuthorize("hasRole('OWNER')")
public class McpDiscoveryController {

    private static final Logger log = LoggerFactory.getLogger(McpDiscoveryController.class);

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * Returns the Claude Desktop MCP configuration JSON.
     *
     * <p>Users copy this into their {@code claude_desktop_config.json} file,
     * replacing {@code YOUR_MCP_TOKEN} with an actual MCP API token
     * generated via the token management endpoints.</p>
     *
     * @return the MCP client configuration snippet
     */
    @GetMapping("/claude-desktop-config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getClaudeDesktopConfig() {
        log.debug("Generating Claude Desktop MCP configuration");

        String baseUrl = "http://localhost:" + serverPort;

        Map<String, Object> serverConfig = Map.of(
                "url", baseUrl + "/mcp/sse",
                "headers", Map.of(
                        "Authorization", "Bearer YOUR_MCP_TOKEN"
                )
        );

        Map<String, Object> config = Map.of(
                "mcpServers", Map.of(
                        "myoffgridai", serverConfig
                ),
                "instructions", "Replace YOUR_MCP_TOKEN with an actual token from POST " + AppConstants.API_MCP_TOKENS
        );

        return ResponseEntity.ok(ApiResponse.success(config));
    }
}
