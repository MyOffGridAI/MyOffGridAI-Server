package com.myoffgridai.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.dto.AuthResponse;
import com.myoffgridai.auth.dto.UserSummaryDto;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.system.dto.InitializeRequest;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SystemController.class)
@Import(TestSecurityConfig.class)
class SystemControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private SystemConfigService systemConfigService;
    @MockitoBean private AuthService authService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;

    // ── Status ──────────────────────────────────────────────────────────────

    @Test
    void getStatus_returnsSystemStatus() throws Exception {
        SystemConfig config = new SystemConfig();
        config.setInitialized(true);
        config.setInstanceName("My Homestead");
        config.setFortressEnabled(false);
        config.setWifiConfigured(true);
        when(systemConfigService.getConfig()).thenReturn(config);

        mockMvc.perform(get("/api/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.initialized").value(true))
                .andExpect(jsonPath("$.data.instanceName").value("My Homestead"))
                .andExpect(jsonPath("$.data.fortressEnabled").value(false))
                .andExpect(jsonPath("$.data.serverVersion").value("1.0.0"));
    }

    // ── Initialize ──────────────────────────────────────────────────────────

    @Test
    void initialize_createsOwnerAccount() throws Exception {
        when(systemConfigService.isInitialized()).thenReturn(false);

        UUID ownerId = UUID.randomUUID();
        UserSummaryDto userSummary = new UserSummaryDto(
                ownerId, "owner", "Owner", Role.ROLE_OWNER, true);
        AuthResponse authResponse = new AuthResponse(
                "access-token", "refresh-token", "Bearer", 86400, userSummary);
        when(authService.register(any())).thenReturn(authResponse);

        InitializeRequest request = new InitializeRequest(
                "My Homestead", "owner", "Owner", "owner@test.com", "password123");

        mockMvc.perform(post("/api/system/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.user.username").value("owner"))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_OWNER"))
                .andExpect(jsonPath("$.message").value("System initialized successfully"));
    }

    @Test
    void initialize_alreadyInitialized_returns409() throws Exception {
        when(systemConfigService.isInitialized()).thenReturn(true);

        InitializeRequest request = new InitializeRequest(
                "My Homestead", "owner", "Owner", "owner@test.com", "password123");

        mockMvc.perform(post("/api/system/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("System is already initialized"));
    }

    @Test
    void initialize_invalidBody_returns400() throws Exception {
        // Missing required fields: instanceName, username, displayName, password
        String invalidBody = "{\"email\":\"owner@test.com\"}";

        mockMvc.perform(post("/api/system/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }
}
