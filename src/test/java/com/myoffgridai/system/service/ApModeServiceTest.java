package com.myoffgridai.system.service;

import com.myoffgridai.system.dto.WifiConnectionStatus;
import com.myoffgridai.system.dto.WifiNetwork;
import com.myoffgridai.system.model.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApModeServiceTest {

    @Mock
    private SystemConfigService systemConfigService;

    private ApModeService apModeService;

    @BeforeEach
    void setUp() {
        // Always mock mode in tests — no real OS commands
        apModeService = new ApModeService(systemConfigService, true);
    }

    @Test
    void startApMode_mockMode_logsWarning() {
        // Should not throw — mock mode skips OS commands
        assertDoesNotThrow(() -> apModeService.startApMode());
    }

    @Test
    void stopApMode_mockMode_logsWarning() {
        assertDoesNotThrow(() -> apModeService.stopApMode());
    }

    @Test
    void isApModeActive_mockMode_returnsConfigValue() {
        SystemConfig config = new SystemConfig();
        config.setApModeEnabled(true);
        when(systemConfigService.getConfig()).thenReturn(config);

        assertTrue(apModeService.isApModeActive());
    }

    @Test
    void isApModeActive_mockMode_returnsFalseWhenDisabled() {
        SystemConfig config = new SystemConfig();
        config.setApModeEnabled(false);
        when(systemConfigService.getConfig()).thenReturn(config);

        assertFalse(apModeService.isApModeActive());
    }

    @Test
    void scanWifiNetworks_mockMode_returnsSampleNetworks() {
        List<WifiNetwork> networks = apModeService.scanWifiNetworks();

        assertNotNull(networks);
        assertEquals(3, networks.size());
        assertEquals("HomeNetwork", networks.get(0).ssid());
        assertEquals(-45, networks.get(0).signalStrength());
        assertEquals("WPA2", networks.get(0).security());
    }

    @Test
    void connectToWifi_mockMode_alwaysReturnsTrue() {
        boolean result = apModeService.connectToWifi("TestNetwork", "password123");

        assertTrue(result);
    }

    @Test
    void getConnectionStatus_mockMode_returnsConnectedNoInternet() {
        WifiConnectionStatus status = apModeService.getConnectionStatus();

        assertNotNull(status);
        assertTrue(status.connected());
        assertFalse(status.hasInternet());
    }
}
