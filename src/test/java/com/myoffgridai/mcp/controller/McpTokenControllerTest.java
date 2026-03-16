package com.myoffgridai.mcp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.mcp.dto.CreateMcpTokenRequest;
import com.myoffgridai.mcp.dto.McpTokenCreateResult;
import com.myoffgridai.mcp.dto.McpTokenSummaryDto;
import com.myoffgridai.mcp.service.McpTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebMvcTest for {@link McpTokenController}.
 */
@WebMvcTest(McpTokenController.class)
@Import(TestSecurityConfig.class)
class McpTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private McpTokenService mcpTokenService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private AuthService authService;

    @MockitoBean
    private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User ownerUser;
    private User memberUser;

    @BeforeEach
    void setUp() {
        ownerUser = new User();
        ownerUser.setId(UUID.randomUUID());
        ownerUser.setUsername("owner");
        ownerUser.setRole(Role.ROLE_OWNER);

        memberUser = new User();
        memberUser.setId(UUID.randomUUID());
        memberUser.setUsername("member");
        memberUser.setRole(Role.ROLE_MEMBER);
    }

    @Test
    void createToken_returnsCreatedWithPlaintext() throws Exception {
        McpTokenCreateResult result = new McpTokenCreateResult(
                UUID.randomUUID(), "mcp_abc123plaintext", "Claude Desktop");
        when(mcpTokenService.createToken(eq(ownerUser.getId()), eq("Claude Desktop")))
                .thenReturn(result);

        CreateMcpTokenRequest request = new CreateMcpTokenRequest("Claude Desktop");

        mockMvc.perform(post("/api/mcp/tokens")
                        .with(user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.plaintextToken").value("mcp_abc123plaintext"))
                .andExpect(jsonPath("$.data.name").value("Claude Desktop"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createToken_returns400ForBlankName() throws Exception {
        CreateMcpTokenRequest request = new CreateMcpTokenRequest("");

        mockMvc.perform(post("/api/mcp/tokens")
                        .with(user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createToken_returns403ForNonOwner() throws Exception {
        CreateMcpTokenRequest request = new CreateMcpTokenRequest("Test Token");

        mockMvc.perform(post("/api/mcp/tokens")
                        .with(user(memberUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createToken_returns401ForUnauthenticated() throws Exception {
        CreateMcpTokenRequest request = new CreateMcpTokenRequest("Test Token");

        mockMvc.perform(post("/api/mcp/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listTokens_returnsTokenSummaries() throws Exception {
        List<McpTokenSummaryDto> tokens = List.of(
                new McpTokenSummaryDto(UUID.randomUUID(), "Token 1", true, null, Instant.now()),
                new McpTokenSummaryDto(UUID.randomUUID(), "Token 2", false, Instant.now(), Instant.now())
        );
        when(mcpTokenService.listTokens(ownerUser.getId())).thenReturn(tokens);

        mockMvc.perform(get("/api/mcp/tokens")
                        .with(user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Token 1"))
                .andExpect(jsonPath("$.data[1].name").value("Token 2"));
    }

    @Test
    void listTokens_returns403ForNonOwner() throws Exception {
        mockMvc.perform(get("/api/mcp/tokens")
                        .with(user(memberUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokeToken_returns204() throws Exception {
        UUID tokenId = UUID.randomUUID();

        mockMvc.perform(delete("/api/mcp/tokens/" + tokenId)
                        .with(user(ownerUser)))
                .andExpect(status().isNoContent());

        verify(mcpTokenService).revokeToken(ownerUser.getId(), tokenId);
    }

    @Test
    void revokeToken_returns403ForNonOwner() throws Exception {
        UUID tokenId = UUID.randomUUID();

        mockMvc.perform(delete("/api/mcp/tokens/" + tokenId)
                        .with(user(memberUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokeToken_returns401ForUnauthenticated() throws Exception {
        UUID tokenId = UUID.randomUUID();

        mockMvc.perform(delete("/api/mcp/tokens/" + tokenId))
                .andExpect(status().isUnauthorized());
    }
}
