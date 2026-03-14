package com.myoffgridai.system.controller;

import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.system.dto.WifiConnectRequest;
import com.myoffgridai.system.dto.WifiConnectionStatus;
import com.myoffgridai.system.dto.WifiNetwork;
import com.myoffgridai.system.service.ApModeService;
import com.myoffgridai.system.service.SystemConfigService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for the captive portal setup wizard.
 *
 * <p>Serves static HTML pages for the 4-step setup wizard and provides
 * JSON API endpoints for WiFi scanning, connection, and status checks.
 * All endpoints are public (no authentication required) since they run
 * before any user accounts exist.</p>
 */
@Controller
public class CaptivePortalController {

    private static final Logger log = LoggerFactory.getLogger(CaptivePortalController.class);

    private final SystemConfigService systemConfigService;
    private final ApModeService apModeService;

    /**
     * Constructs the captive portal controller.
     *
     * @param systemConfigService the system config service
     * @param apModeService       the AP mode service for WiFi operations
     */
    public CaptivePortalController(SystemConfigService systemConfigService,
                                   ApModeService apModeService) {
        this.systemConfigService = systemConfigService;
        this.apModeService = apModeService;
    }

    /**
     * Serves the welcome page (Step 1) of the setup wizard.
     * Redirects to "/" if the system is already initialized.
     *
     * @return forward to index.html or redirect to /
     */
    @GetMapping("/setup")
    public String setupWelcome() {
        if (systemConfigService.isInitialized()) {
            log.info("System already initialized, redirecting from /setup to /");
            return "redirect:/";
        }
        return "forward:/setup/index.html";
    }

    /**
     * Serves the WiFi configuration page (Step 2).
     *
     * @return forward to wifi.html
     */
    @GetMapping("/setup/wifi")
    public String setupWifi() {
        return "forward:/setup/wifi.html";
    }

    /**
     * Serves the account creation page (Step 3).
     *
     * @return forward to account.html
     */
    @GetMapping("/setup/account")
    public String setupAccount() {
        return "forward:/setup/account.html";
    }

    /**
     * Serves the confirmation page (Step 4).
     *
     * @return forward to confirm.html
     */
    @GetMapping("/setup/confirm")
    public String setupConfirm() {
        return "forward:/setup/confirm.html";
    }

    /**
     * Scans for available WiFi networks. Returns a JSON list of discovered
     * networks with SSID, signal strength, and security type.
     *
     * @return the list of WiFi networks
     */
    @GetMapping("/api/setup/wifi/scan")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<WifiNetwork>>> scanWifi() {
        log.info("WiFi scan requested");
        List<WifiNetwork> networks = apModeService.scanWifiNetworks();
        return ResponseEntity.ok(ApiResponse.success(networks));
    }

    /**
     * Connects the device to a WiFi network.
     *
     * @param request the WiFi connection request with SSID and password
     * @return success or failure with descriptive message
     */
    @PostMapping("/api/setup/wifi/connect")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> connectWifi(
            @Valid @RequestBody WifiConnectRequest request) {
        log.info("WiFi connect requested for SSID: {}", request.ssid());
        boolean success = apModeService.connectToWifi(request.ssid(), request.password());

        Map<String, Object> result = Map.of(
                "success", success,
                "message", success
                        ? "Connected to " + request.ssid()
                        : "Failed to connect. Wrong password or network unavailable."
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Returns the current WiFi connection status.
     *
     * @return the connection status
     */
    @GetMapping("/api/setup/wifi/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<WifiConnectionStatus>> wifiStatus() {
        WifiConnectionStatus status = apModeService.getConnectionStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
