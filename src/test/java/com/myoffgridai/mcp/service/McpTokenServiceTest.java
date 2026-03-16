package com.myoffgridai.mcp.service;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.mcp.dto.McpTokenCreateResult;
import com.myoffgridai.mcp.dto.McpTokenSummaryDto;
import com.myoffgridai.mcp.model.McpApiToken;
import com.myoffgridai.mcp.repository.McpApiTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link McpTokenService}.
 */
@ExtendWith(MockitoExtension.class)
class McpTokenServiceTest {

    @Mock
    private McpApiTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private McpTokenService mcpTokenService;

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID TOKEN_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mcpTokenService = new McpTokenService(tokenRepository, passwordEncoder);
    }

    @Test
    void createToken_savesHashedTokenAndReturnsPlaintext() {
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedvalue");
        when(tokenRepository.save(any(McpApiToken.class))).thenAnswer(invocation -> {
            McpApiToken token = invocation.getArgument(0);
            token.setId(TOKEN_ID);
            return token;
        });

        McpTokenCreateResult result = mcpTokenService.createToken(OWNER_ID, "Test Token");

        assertNotNull(result);
        assertEquals(TOKEN_ID, result.tokenId());
        assertEquals("Test Token", result.name());
        assertNotNull(result.plaintextToken());
        assertFalse(result.plaintextToken().isBlank());

        ArgumentCaptor<McpApiToken> captor = ArgumentCaptor.forClass(McpApiToken.class);
        verify(tokenRepository).save(captor.capture());
        McpApiToken saved = captor.getValue();
        assertEquals("$2a$10$hashedvalue", saved.getTokenHash());
        assertEquals(OWNER_ID, saved.getCreatedBy());
        assertEquals("Test Token", saved.getName());
        assertTrue(saved.isActive());
    }

    @Test
    void validateToken_returnsMatchingToken() {
        McpApiToken token = createTestToken();
        when(tokenRepository.findByIsActiveTrue()).thenReturn(List.of(token));
        when(passwordEncoder.matches("plaintext", token.getTokenHash())).thenReturn(true);

        McpApiToken result = mcpTokenService.validateToken("plaintext");

        assertNotNull(result);
        assertEquals(TOKEN_ID, result.getId());
    }

    @Test
    void validateToken_returnsNullForNoMatch() {
        McpApiToken token = createTestToken();
        when(tokenRepository.findByIsActiveTrue()).thenReturn(List.of(token));
        when(passwordEncoder.matches("wrong-token", token.getTokenHash())).thenReturn(false);

        McpApiToken result = mcpTokenService.validateToken("wrong-token");

        assertNull(result);
    }

    @Test
    void validateToken_returnsNullWhenNoActiveTokens() {
        when(tokenRepository.findByIsActiveTrue()).thenReturn(List.of());

        McpApiToken result = mcpTokenService.validateToken("some-token");

        assertNull(result);
    }

    @Test
    void updateLastUsed_updatesTimestamp() {
        McpApiToken token = createTestToken();
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(tokenRepository.save(any(McpApiToken.class))).thenAnswer(i -> i.getArgument(0));

        mcpTokenService.updateLastUsed(TOKEN_ID);

        verify(tokenRepository).save(token);
        assertNotNull(token.getLastUsedAt());
    }

    @Test
    void updateLastUsed_doesNothingIfTokenNotFound() {
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.empty());

        mcpTokenService.updateLastUsed(TOKEN_ID);

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void listTokens_returnsSummaryDtos() {
        McpApiToken token = createTestToken();
        when(tokenRepository.findByCreatedByOrderByCreatedAtDesc(OWNER_ID)).thenReturn(List.of(token));

        List<McpTokenSummaryDto> results = mcpTokenService.listTokens(OWNER_ID);

        assertEquals(1, results.size());
        assertEquals(TOKEN_ID, results.get(0).id());
        assertEquals("Test Token", results.get(0).name());
        assertTrue(results.get(0).isActive());
    }

    @Test
    void listTokens_returnsEmptyListForNoTokens() {
        when(tokenRepository.findByCreatedByOrderByCreatedAtDesc(OWNER_ID)).thenReturn(List.of());

        List<McpTokenSummaryDto> results = mcpTokenService.listTokens(OWNER_ID);

        assertTrue(results.isEmpty());
    }

    @Test
    void revokeToken_deactivatesOwnedToken() {
        McpApiToken token = createTestToken();
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(tokenRepository.save(any(McpApiToken.class))).thenAnswer(i -> i.getArgument(0));

        mcpTokenService.revokeToken(OWNER_ID, TOKEN_ID);

        assertFalse(token.isActive());
        verify(tokenRepository).save(token);
    }

    @Test
    void revokeToken_throwsIfTokenNotFound() {
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> mcpTokenService.revokeToken(OWNER_ID, TOKEN_ID));
    }

    @Test
    void revokeToken_throwsIfTokenNotOwnedByUser() {
        McpApiToken token = createTestToken();
        token.setCreatedBy(UUID.randomUUID()); // Different owner
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));

        assertThrows(EntityNotFoundException.class,
                () -> mcpTokenService.revokeToken(OWNER_ID, TOKEN_ID));
    }

    private McpApiToken createTestToken() {
        McpApiToken token = new McpApiToken();
        token.setId(TOKEN_ID);
        token.setName("Test Token");
        token.setCreatedBy(OWNER_ID);
        token.setTokenHash("$2a$10$hashedvalue");
        token.setActive(true);
        token.setCreatedAt(Instant.now());
        return token;
    }
}
