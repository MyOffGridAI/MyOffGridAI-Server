package com.myoffgridai.mcp.config;

import com.myoffgridai.mcp.model.McpApiToken;
import com.myoffgridai.mcp.service.McpTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link McpAuthFilter}.
 */
@ExtendWith(MockitoExtension.class)
class McpAuthFilterTest {

    @Mock
    private McpTokenService mcpTokenService;

    @Mock
    private FilterChain filterChain;

    private McpAuthFilter mcpAuthFilter;

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID TOKEN_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mcpAuthFilter = new McpAuthFilter(mcpTokenService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilter_returnsTrueForNonMcpPaths() throws ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        assertTrue(mcpAuthFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_returnsFalseForMcpPaths() throws ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mcp/sse");
        assertFalse(mcpAuthFilter.shouldNotFilter(request));
    }

    @Test
    void doFilterInternal_setsAuthenticationForValidToken() throws ServletException, IOException {
        McpApiToken token = createTestToken();
        when(mcpTokenService.validateToken("valid-token")).thenReturn(token);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mcp/sse");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        mcpAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertInstanceOf(McpAuthentication.class, SecurityContextHolder.getContext().getAuthentication());

        McpAuthentication auth = (McpAuthentication) SecurityContextHolder.getContext().getAuthentication();
        assertEquals(OWNER_ID, auth.getOwnerUserId());
        assertEquals(TOKEN_ID, auth.getTokenId());
        assertTrue(auth.isAuthenticated());
    }

    @Test
    void doFilterInternal_updatesLastUsedTimestamp() throws ServletException, IOException {
        McpApiToken token = createTestToken();
        when(mcpTokenService.validateToken("valid-token")).thenReturn(token);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mcp/sse");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        mcpAuthFilter.doFilterInternal(request, response, filterChain);

        verify(mcpTokenService).updateLastUsed(TOKEN_ID);
    }

    @Test
    void doFilterInternal_returns401ForMissingHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mcp/sse");
        MockHttpServletResponse response = new MockHttpServletResponse();

        mcpAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Missing or invalid Authorization header"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_returns401ForNonBearerHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mcp/sse");
        request.addHeader("Authorization", "Basic abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        mcpAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_returns401ForInvalidToken() throws ServletException, IOException {
        when(mcpTokenService.validateToken("invalid-token")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mcp/sse");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        mcpAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid MCP API token"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_continuesEvenIfUpdateLastUsedFails() throws ServletException, IOException {
        McpApiToken token = createTestToken();
        when(mcpTokenService.validateToken("valid-token")).thenReturn(token);
        doThrow(new RuntimeException("DB error")).when(mcpTokenService).updateLastUsed(TOKEN_ID);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mcp/sse");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        mcpAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
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
