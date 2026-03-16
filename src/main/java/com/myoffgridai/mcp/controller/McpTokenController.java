package com.myoffgridai.mcp.controller;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.mcp.dto.CreateMcpTokenRequest;
import com.myoffgridai.mcp.dto.McpTokenCreateResult;
import com.myoffgridai.mcp.dto.McpTokenSummaryDto;
import com.myoffgridai.mcp.service.McpTokenService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing MCP API tokens.
 *
 * <p>Allows OWNER users to create, list, and revoke MCP API tokens
 * used by external AI clients (Claude Desktop, Claude Code) to
 * authenticate against the MCP SSE endpoints.</p>
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@RestController
@RequestMapping(AppConstants.API_MCP_TOKENS)
@PreAuthorize("hasRole('OWNER')")
public class McpTokenController {

    private static final Logger log = LoggerFactory.getLogger(McpTokenController.class);

    private final McpTokenService mcpTokenService;

    /**
     * Constructs the controller with the MCP token service.
     *
     * @param mcpTokenService the MCP token service
     */
    public McpTokenController(McpTokenService mcpTokenService) {
        this.mcpTokenService = mcpTokenService;
    }

    /**
     * Creates a new MCP API token. The plaintext token is returned exactly once.
     *
     * @param principal the authenticated OWNER user
     * @param request   the token creation request containing a name
     * @return the created token with its plaintext (shown once only)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<McpTokenCreateResult>> createToken(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody CreateMcpTokenRequest request) {
        log.info("User {} creating MCP token '{}'", principal.getId(), request.name());
        McpTokenCreateResult result = mcpTokenService.createToken(principal.getId(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "MCP token created. Save the token — it will not be shown again."));
    }

    /**
     * Lists all MCP API tokens for the authenticated OWNER.
     *
     * @param principal the authenticated OWNER user
     * @return list of token summaries (never includes plaintext or hash)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<McpTokenSummaryDto>>> listTokens(
            @AuthenticationPrincipal User principal) {
        List<McpTokenSummaryDto> tokens = mcpTokenService.listTokens(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    /**
     * Revokes (deactivates) an MCP API token.
     *
     * @param principal the authenticated OWNER user
     * @param tokenId   the token ID to revoke
     * @return 204 No Content on success
     */
    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Void> revokeToken(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID tokenId) {
        log.info("User {} revoking MCP token {}", principal.getId(), tokenId);
        mcpTokenService.revokeToken(principal.getId(), tokenId);
        return ResponseEntity.noContent().build();
    }
}
