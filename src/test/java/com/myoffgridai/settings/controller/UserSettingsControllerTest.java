package com.myoffgridai.settings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.settings.dto.UpdateUserSettingsRequest;
import com.myoffgridai.settings.dto.UserSettingsDto;
import com.myoffgridai.settings.service.UserSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for {@link UserSettingsController}.
 */
@WebMvcTest(UserSettingsController.class)
@Import(TestSecurityConfig.class)
class UserSettingsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserSettingsService userSettingsService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    @MockitoBean private AuthService authService;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setRole(Role.ROLE_MEMBER);
        testUser.setPasswordHash("hash");
    }

    // ── GET /api/users/me/settings ──────────────────────────────────────────

    @Test
    void getSettings_returnsSettingsForAuthenticatedUser() throws Exception {
        UserSettingsDto dto = new UserSettingsDto("dark");
        when(userSettingsService.getSettings(userId)).thenReturn(dto);

        mockMvc.perform(get("/api/users/me/settings")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.themePreference").value("dark"));

        verify(userSettingsService).getSettings(userId);
    }

    @Test
    void getSettings_returns401ForUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/users/me/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSettings_worksForOwnerRole() throws Exception {
        testUser.setRole(Role.ROLE_OWNER);
        UserSettingsDto dto = new UserSettingsDto("system");
        when(userSettingsService.getSettings(userId)).thenReturn(dto);

        mockMvc.perform(get("/api/users/me/settings")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.themePreference").value("system"));
    }

    // ── PUT /api/users/me/settings ──────────────────────────────────────────

    @Test
    void updateSettings_updatesAndReturnsSettings() throws Exception {
        UpdateUserSettingsRequest request = new UpdateUserSettingsRequest("dark");
        UserSettingsDto dto = new UserSettingsDto("dark");
        when(userSettingsService.updateSettings(eq(userId), any())).thenReturn(dto);

        mockMvc.perform(put("/api/users/me/settings")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.themePreference").value("dark"))
                .andExpect(jsonPath("$.message").value("User settings updated"));

        verify(userSettingsService).updateSettings(eq(userId), any());
    }

    @Test
    void updateSettings_returns401ForUnauthenticated() throws Exception {
        UpdateUserSettingsRequest request = new UpdateUserSettingsRequest("dark");

        mockMvc.perform(put("/api/users/me/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateSettings_rejectsInvalidThemePreference() throws Exception {
        String invalidJson = "{\"themePreference\":\"neon\"}";

        mockMvc.perform(put("/api/users/me/settings")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSettings_acceptsLightTheme() throws Exception {
        UpdateUserSettingsRequest request = new UpdateUserSettingsRequest("light");
        UserSettingsDto dto = new UserSettingsDto("light");
        when(userSettingsService.updateSettings(eq(userId), any())).thenReturn(dto);

        mockMvc.perform(put("/api/users/me/settings")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.themePreference").value("light"));
    }

    @Test
    void updateSettings_acceptsSystemTheme() throws Exception {
        UpdateUserSettingsRequest request = new UpdateUserSettingsRequest("system");
        UserSettingsDto dto = new UserSettingsDto("system");
        when(userSettingsService.updateSettings(eq(userId), any())).thenReturn(dto);

        mockMvc.perform(put("/api/users/me/settings")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.themePreference").value("system"));
    }
}
