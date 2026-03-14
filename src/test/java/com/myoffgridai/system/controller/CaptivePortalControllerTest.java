package com.myoffgridai.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.system.dto.WifiConnectRequest;
import com.myoffgridai.system.dto.WifiConnectionStatus;
import com.myoffgridai.system.dto.WifiNetwork;
import com.myoffgridai.system.service.ApModeService;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CaptivePortalController.class)
@Import(TestSecurityConfig.class)
class CaptivePortalControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private SystemConfigService systemConfigService;
    @MockitoBean private ApModeService apModeService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    @Test
    void getSetup_uninitializedSystem_returnsForward() throws Exception {
        when(systemConfigService.isInitialized()).thenReturn(false);

        mockMvc.perform(get("/setup"))
                .andExpect(status().isOk());
    }

    @Test
    void getSetup_initializedSystem_redirectsToRoot() throws Exception {
        when(systemConfigService.isInitialized()).thenReturn(true);

        mockMvc.perform(get("/setup"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void scanWifi_returnsNetworkList() throws Exception {
        List<WifiNetwork> networks = List.of(
                new WifiNetwork("HomeNet", -50, "WPA2"),
                new WifiNetwork("OpenNet", -75, "Open")
        );
        when(apModeService.scanWifiNetworks()).thenReturn(networks);

        mockMvc.perform(get("/api/setup/wifi/scan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].ssid").value("HomeNet"))
                .andExpect(jsonPath("$.data[0].signalStrength").value(-50))
                .andExpect(jsonPath("$.data[1].ssid").value("OpenNet"));
    }

    @Test
    void connectWifi_success_returnsSuccessMessage() throws Exception {
        when(apModeService.connectToWifi("HomeNet", "password123")).thenReturn(true);

        WifiConnectRequest request = new WifiConnectRequest("HomeNet", "password123");

        mockMvc.perform(post("/api/setup/wifi/connect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Connected to HomeNet"));
    }

    @Test
    void connectWifi_failure_returnsFailureMessage() throws Exception {
        when(apModeService.connectToWifi("HomeNet", "wrongpass")).thenReturn(false);

        WifiConnectRequest request = new WifiConnectRequest("HomeNet", "wrongpass");

        mockMvc.perform(post("/api/setup/wifi/connect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(false));
    }

    @Test
    void wifiStatus_returnsConnectionStatus() throws Exception {
        when(apModeService.getConnectionStatus())
                .thenReturn(new WifiConnectionStatus(true, false));

        mockMvc.perform(get("/api/setup/wifi/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.connected").value(true))
                .andExpect(jsonPath("$.data.hasInternet").value(false));
    }

    @Test
    void scanWifi_noAuthRequired() throws Exception {
        when(apModeService.scanWifiNetworks()).thenReturn(List.of());

        mockMvc.perform(get("/api/setup/wifi/scan"))
                .andExpect(status().isOk());
    }
}
