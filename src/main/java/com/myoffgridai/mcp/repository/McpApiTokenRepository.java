package com.myoffgridai.mcp.repository;

import com.myoffgridai.mcp.model.McpApiToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link McpApiToken} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public interface McpApiTokenRepository extends JpaRepository<McpApiToken, UUID> {

    /**
     * Finds all active tokens, used for token validation against BCrypt hashes.
     *
     * @return list of active MCP API tokens
     */
    List<McpApiToken> findByIsActiveTrue();

    /**
     * Finds all tokens created by a specific user.
     *
     * @param createdBy the owner user's ID
     * @return list of MCP API tokens for the user
     */
    List<McpApiToken> findByCreatedByOrderByCreatedAtDesc(UUID createdBy);
}
