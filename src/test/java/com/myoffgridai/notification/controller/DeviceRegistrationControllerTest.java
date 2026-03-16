package com.myoffgridai.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.notification.dto.DeviceRegistrationDto;
import com.myoffgridai.notification.dto.RegisterDeviceRequest;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.notification.service.DeviceRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link DeviceRegistrationController}.
 */
@WebMvcTest(DeviceRegistrationController.class)
@Import(TestSecurityConfig.class)
class DeviceRegistrationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private DeviceRegistrationService deviceRegistrationService;
    @MockBean private JwtService jwtService;
    @MockBean private AuthService authService;
    @MockBean private UserDetailsService userDetailsService;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User createUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setRole(Role.ROLE_MEMBER);
        return user;
    }

    private org.springframework.security.authentication.UsernamePasswordAuthenticationToken
    createAuth(User user) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
    }

    @Test
    void registerDevice_authenticated_returnsOk() throws Exception {
        User user = createUser();
        DeviceRegistrationDto dto = new DeviceRegistrationDto(
                UUID.randomUUID(), "device-1", "Phone", "android",
                "mqtt-1", Instant.now());
        when(deviceRegistrationService.registerDevice(eq(user.getId()), any()))
                .thenReturn(dto);

        RegisterDeviceRequest request = new RegisterDeviceRequest(
                "device-1", "Phone", "android", "mqtt-1");

        mockMvc.perform(post("/api/notifications/devices")
                        .with(authentication(createAuth(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deviceId").value("device-1"));
    }

    @Test
    void registerDevice_unauthenticated_returns401() throws Exception {
        RegisterDeviceRequest request = new RegisterDeviceRequest(
                "device-1", "Phone", "android", "mqtt-1");

        mockMvc.perform(post("/api/notifications/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDevices_authenticated_returnsOk() throws Exception {
        User user = createUser();
        when(deviceRegistrationService.getDevicesForUser(user.getId())).thenReturn(List.of());

        mockMvc.perform(get("/api/notifications/devices")
                        .with(authentication(createAuth(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unregisterDevice_authenticated_returnsOk() throws Exception {
        User user = createUser();

        mockMvc.perform(delete("/api/notifications/devices/device-1")
                        .with(authentication(createAuth(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(deviceRegistrationService).unregisterDevice(user.getId(), "device-1");
    }

    @Test
    void unregisterDevice_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/notifications/devices/device-1"))
                .andExpect(status().isUnauthorized());
    }
}
