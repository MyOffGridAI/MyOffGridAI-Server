package com.myoffgridai.ai.controller;

import com.myoffgridai.ai.dto.OllamaModelInfo;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModelController.class)
@Import(TestSecurityConfig.class)
class ModelControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private OllamaService ollamaService;
    @MockBean private JwtService jwtService;
    @MockBean private AuthService authService;
    @MockBean private UserDetailsService userDetailsService;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setRole(Role.ROLE_MEMBER);
        testUser.setPasswordHash("$2a$10$dummy");
        testUser.setIsActive(true);
    }

    @Test
    void listModels_public_returns200() throws Exception {
        when(ollamaService.listModels()).thenReturn(List.of(
                new OllamaModelInfo("qwen3:32b", 17_000_000_000L, Instant.now())));

        mockMvc.perform(get("/api/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("qwen3:32b"));
    }

    @Test
    void getHealth_public_returns200() throws Exception {
        when(ollamaService.isAvailable()).thenReturn(true);

        mockMvc.perform(get("/api/models/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.activeModel").exists());
    }

    @Test
    void getHealth_ollamaDown_returnsAvailableFalse() throws Exception {
        when(ollamaService.isAvailable()).thenReturn(false);

        mockMvc.perform(get("/api/models/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void getActiveModel_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/api/models/active")
                        .with(authentication(createAuth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modelName").value("qwen3:32b"));
    }

    @Test
    void getActiveModel_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/models/active"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.security.authentication.UsernamePasswordAuthenticationToken
    createAuth(User user) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
    }
}
