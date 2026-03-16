package com.myoffgridai.mcp.controller;

import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebMvcTest for {@link McpDiscoveryController}.
 */
@WebMvcTest(McpDiscoveryController.class)
@Import(TestSecurityConfig.class)
class McpDiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    void getClaudeDesktopConfig_returnsConfigForOwner() throws Exception {
        mockMvc.perform(get("/api/mcp/claude-desktop-config")
                        .with(user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mcpServers.myoffgridai.url").exists())
                .andExpect(jsonPath("$.data.mcpServers.myoffgridai.headers.Authorization").value("Bearer YOUR_MCP_TOKEN"))
                .andExpect(jsonPath("$.data.instructions").exists());
    }

    @Test
    void getClaudeDesktopConfig_returns403ForNonOwner() throws Exception {
        mockMvc.perform(get("/api/mcp/claude-desktop-config")
                        .with(user(memberUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getClaudeDesktopConfig_returns401ForUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/mcp/claude-desktop-config"))
                .andExpect(status().isUnauthorized());
    }
}
