package com.myoffgridai.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.FactoryResetService;
import com.myoffgridai.system.service.NetworkTransitionService;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for the AI settings endpoints on {@link SystemController}.
 */
@WebMvcTest(SystemController.class)
@Import(TestSecurityConfig.class)
class SystemControllerAiSettingsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private SystemConfigService systemConfigService;
    @MockitoBean private AuthService authService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    @MockitoBean private NetworkTransitionService networkTransitionService;
    @MockitoBean private FactoryResetService factoryResetService;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    @Test
    @WithMockUser(roles = "OWNER")
    void getAiSettings_returns200() throws Exception {
        AiSettingsDto settings = new AiSettingsDto("test-model", 0.7, 0.45, 5, 2048);
        when(systemConfigService.getAiSettings()).thenReturn(settings);

        mockMvc.perform(get("/api/system/ai-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.modelName").value("test-model"))
                .andExpect(jsonPath("$.data.temperature").value(0.7))
                .andExpect(jsonPath("$.data.similarityThreshold").value(0.45))
                .andExpect(jsonPath("$.data.memoryTopK").value(5))
                .andExpect(jsonPath("$.data.ragMaxContextTokens").value(2048));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAiSettings_adminRole_returns200() throws Exception {
        AiSettingsDto settings = new AiSettingsDto("test-model", 1.0, 0.5, 10, 4096);
        when(systemConfigService.getAiSettings()).thenReturn(settings);

        mockMvc.perform(get("/api/system/ai-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temperature").value(1.0));
    }

    @Test
    void getAiSettings_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/system/ai-settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void getAiSettings_memberRole_returns403() throws Exception {
        mockMvc.perform(get("/api/system/ai-settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void updateAiSettings_returns200() throws Exception {
        AiSettingsDto updated = new AiSettingsDto("test-model", 1.2, 0.6, 8, 4096);
        when(systemConfigService.updateAiSettings(any(AiSettingsDto.class))).thenReturn(updated);

        AiSettingsDto request = new AiSettingsDto("test-model", 1.2, 0.6, 8, 4096);

        mockMvc.perform(put("/api/system/ai-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.temperature").value(1.2))
                .andExpect(jsonPath("$.data.similarityThreshold").value(0.6))
                .andExpect(jsonPath("$.data.memoryTopK").value(8))
                .andExpect(jsonPath("$.data.ragMaxContextTokens").value(4096))
                .andExpect(jsonPath("$.message").value("AI settings updated successfully"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void updateAiSettings_invalidValues_returns400() throws Exception {
        when(systemConfigService.updateAiSettings(any(AiSettingsDto.class)))
                .thenThrow(new IllegalArgumentException("Temperature must be between 0.0 and 2.0"));

        AiSettingsDto request = new AiSettingsDto(null, 3.0, null, null, null);

        mockMvc.perform(put("/api/system/ai-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Temperature must be between 0.0 and 2.0"));
    }

    @Test
    void updateAiSettings_noAuth_returns401() throws Exception {
        AiSettingsDto request = new AiSettingsDto("test-model", 1.0, 0.5, 5, 2048);

        mockMvc.perform(put("/api/system/ai-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
