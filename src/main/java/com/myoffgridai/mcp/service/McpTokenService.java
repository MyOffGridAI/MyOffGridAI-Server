package com.myoffgridai.mcp.service;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.mcp.dto.McpTokenCreateResult;
import com.myoffgridai.mcp.dto.McpTokenSummaryDto;
import com.myoffgridai.mcp.model.McpApiToken;
import com.myoffgridai.mcp.repository.McpApiTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing MCP API tokens used by external AI clients.
 *
 * <p>Tokens are random 32-byte secrets that are BCrypt-hashed before storage.
 * The plaintext is returned exactly once at creation time. Validation iterates
 * all active tokens and checks BCrypt hashes (suitable for small token counts).</p>
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Service
public class McpTokenService {

    private static final Logger log = LoggerFactory.getLogger(McpTokenService.class);
    private static final int TOKEN_BYTES = 32;

    private final McpApiTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs the service with required dependencies.
     *
     * @param tokenRepository the MCP token repository
     * @param passwordEncoder the BCrypt password encoder
     */
    public McpTokenService(McpApiTokenRepository tokenRepository,
                           PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a new MCP API token for the given owner.
     *
     * @param ownerId the OWNER user who is creating the token
     * @param name    human-readable label for the token
     * @return the create result with the plaintext token (shown once only)
     */
    @Transactional
    public McpTokenCreateResult createToken(UUID ownerId, String name) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(randomBytes);
        String plaintext = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        McpApiToken token = new McpApiToken();
        token.setName(name);
        token.setCreatedBy(ownerId);
        token.setTokenHash(passwordEncoder.encode(plaintext));
        token.setActive(true);
        token = tokenRepository.save(token);

        log.info("Created MCP API token '{}' for owner {}", name, ownerId);
        return new McpTokenCreateResult(token.getId(), plaintext, name);
    }

    /**
     * Validates a plaintext token against all active token hashes.
     *
     * @param plaintext the token to validate
     * @return the matching {@link McpApiToken} if valid, or null if invalid
     */
    @Transactional(readOnly = true)
    public McpApiToken validateToken(String plaintext) {
        List<McpApiToken> activeTokens = tokenRepository.findByIsActiveTrue();
        for (McpApiToken token : activeTokens) {
            if (passwordEncoder.matches(plaintext, token.getTokenHash())) {
                return token;
            }
        }
        return null;
    }

    /**
     * Updates the last-used timestamp for a token.
     *
     * @param tokenId the token ID
     */
    @Transactional
    public void updateLastUsed(UUID tokenId) {
        tokenRepository.findById(tokenId).ifPresent(token -> {
            token.setLastUsedAt(Instant.now());
            tokenRepository.save(token);
        });
    }

    /**
     * Lists all tokens created by the given owner.
     *
     * @param ownerId the owner user's ID
     * @return list of token summaries (never includes plaintext or hash)
     */
    @Transactional(readOnly = true)
    public List<McpTokenSummaryDto> listTokens(UUID ownerId) {
        return tokenRepository.findByCreatedByOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(McpTokenSummaryDto::from)
                .toList();
    }

    /**
     * Revokes (deactivates) a token.
     *
     * @param ownerId the owner user's ID (must match the token's creator)
     * @param tokenId the token to revoke
     * @throws EntityNotFoundException if the token is not found or not owned by the user
     */
    @Transactional
    public void revokeToken(UUID ownerId, UUID tokenId) {
        McpApiToken token = tokenRepository.findById(tokenId)
                .filter(t -> t.getCreatedBy().equals(ownerId))
                .orElseThrow(() -> new EntityNotFoundException("MCP token not found: " + tokenId));
        token.setActive(false);
        tokenRepository.save(token);
        log.info("Revoked MCP API token '{}' ({})", token.getName(), tokenId);
    }
}
