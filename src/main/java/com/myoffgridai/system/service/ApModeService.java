package com.myoffgridai.system.service;

import com.myoffgridai.common.exception.ApModeException;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.system.dto.WifiConnectionStatus;
import com.myoffgridai.system.dto.WifiNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages the device's Access Point mode for first-boot captive portal setup.
 *
 * <p>Controls hostapd and dnsmasq via systemctl, scans for WiFi networks via
 * nmcli, and handles WiFi connection. In mock mode ({@code app.ap.mock=true}),
 * no OS commands are executed; the service simulates responses for
 * development and testing.</p>
 */
@Service
public class ApModeService {

    private static final Logger log = LoggerFactory.getLogger(ApModeService.class);

    private final SystemConfigService systemConfigService;
    private final boolean mockMode;

    /**
     * Constructs the AP mode service.
     *
     * @param systemConfigService the system config service for reading device state
     * @param mockMode            whether to run in mock mode (no OS commands)
     */
    public ApModeService(SystemConfigService systemConfigService,
                         @Value("${app.ap.mock:true}") boolean mockMode) {
        this.systemConfigService = systemConfigService;
        this.mockMode = mockMode;
        log.info("ApModeService initialized, mock mode: {}", mockMode);
    }

    /**
     * Starts Access Point mode by enabling hostapd and dnsmasq services.
     *
     * <p>Waits up to {@link AppConstants#AP_MODE_START_TIMEOUT_SECONDS} seconds
     * for both services to become active. In mock mode, skips OS commands and
     * logs a warning.</p>
     *
     * @throws ApModeException if the services fail to start
     */
    public void startApMode() {
        if (mockMode) {
            log.warn("AP mode simulated (mock mode)");
            return;
        }

        try {
            executeCommand("sudo systemctl start hostapd");
            executeCommand("sudo systemctl start dnsmasq");

            // Wait for services to become active
            long deadline = System.currentTimeMillis()
                    + (AppConstants.AP_MODE_START_TIMEOUT_SECONDS * 1000L);
            while (System.currentTimeMillis() < deadline) {
                if (isServiceActive("hostapd") && isServiceActive("dnsmasq")) {
                    log.info("AP mode started — broadcasting SSID: {}", AppConstants.AP_MODE_SSID);
                    return;
                }
                Thread.sleep(500);
            }
            throw new ApModeException("AP mode services did not become active within timeout");
        } catch (ApModeException e) {
            throw e;
        } catch (Exception e) {
            throw new ApModeException("Failed to start AP mode: " + e.getMessage(), e);
        }
    }

    /**
     * Stops Access Point mode by disabling hostapd and dnsmasq services.
     *
     * <p>Safe to call even if AP mode is not running. In mock mode, skips
     * OS commands and logs a warning.</p>
     */
    public void stopApMode() {
        if (mockMode) {
            log.warn("AP mode stop simulated (mock mode)");
            return;
        }

        try {
            executeCommand("sudo systemctl stop hostapd");
            executeCommand("sudo systemctl stop dnsmasq");
            log.info("AP mode stopped");
        } catch (Exception e) {
            log.warn("Failed to stop AP mode (may not have been running): {}", e.getMessage());
        }
    }

    /**
     * Checks whether AP mode is currently active.
     *
     * <p>In mock mode, returns the {@code apModeEnabled} flag from
     * {@link com.myoffgridai.system.model.SystemConfig}.</p>
     *
     * @return true if hostapd is running (or apModeEnabled in mock mode)
     */
    public boolean isApModeActive() {
        if (mockMode) {
            return systemConfigService.getConfig().isApModeEnabled();
        }
        return isServiceActive("hostapd");
    }

    /**
     * Scans for available WiFi networks using nmcli.
     *
     * <p>Filters out empty SSIDs and the device's own AP SSID. Returns an
     * empty list on error (never throws). In mock mode, returns a hardcoded
     * list of sample networks.</p>
     *
     * @return list of discovered WiFi networks
     */
    public List<WifiNetwork> scanWifiNetworks() {
        if (mockMode) {
            log.warn("WiFi scan simulated (mock mode)");
            return List.of(
                    new WifiNetwork("HomeNetwork", -45, "WPA2"),
                    new WifiNetwork("Neighbor-5G", -72, "WPA3"),
                    new WifiNetwork("OpenCafe", -80, "Open")
            );
        }

        try {
            String output = executeCommand("sudo nmcli -t -f SSID,SIGNAL,SECURITY device wifi list");
            List<WifiNetwork> networks = new ArrayList<>();

            for (String line : output.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                String[] parts = trimmed.split(":");
                if (parts.length < 3) {
                    continue;
                }

                String ssid = parts[0].trim();
                if (ssid.isEmpty() || ssid.equals(AppConstants.AP_MODE_SSID)) {
                    continue;
                }

                int signal;
                try {
                    signal = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    signal = 0;
                }

                String security = parts[2].trim();
                networks.add(new WifiNetwork(ssid, signal, security));
            }

            return networks;
        } catch (Exception e) {
            log.error("WiFi scan failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Connects the device to a WiFi network using nmcli.
     *
     * <p>In mock mode, always returns true and logs a warning.</p>
     *
     * @param ssid     the network name to connect to
     * @param password the network password
     * @return true if connection succeeded, false otherwise
     */
    public boolean connectToWifi(String ssid, String password) {
        if (mockMode) {
            log.warn("WiFi connect simulated (mock mode) — SSID: {}", ssid);
            return true;
        }

        try {
            String command = String.format(
                    "sudo nmcli device wifi connect \"%s\" password \"%s\"", ssid, password);
            executeCommand(command);
            log.info("Connected to WiFi network: {}", ssid);
            return true;
        } catch (ApModeException e) {
            log.info("Failed to connect to WiFi network {}: {}", ssid, e.getMessage());
            return false;
        }
    }

    /**
     * Returns the current WiFi connection status.
     *
     * <p>In mock mode, returns connected=true, hasInternet=false.</p>
     *
     * @return the WiFi connection status
     */
    public WifiConnectionStatus getConnectionStatus() {
        if (mockMode) {
            log.warn("WiFi status simulated (mock mode)");
            return new WifiConnectionStatus(true, false);
        }

        try {
            String output = executeCommand("nmcli -t -f STATE,CONNECTIVITY general status");
            String trimmed = output.trim();
            String[] parts = trimmed.split(":");
            boolean connected = parts.length > 0 && "connected".equalsIgnoreCase(parts[0].trim());
            boolean hasInternet = parts.length > 1 && "full".equalsIgnoreCase(parts[1].trim());
            return new WifiConnectionStatus(connected, hasInternet);
        } catch (Exception e) {
            log.warn("Failed to get connection status: {}", e.getMessage());
            return new WifiConnectionStatus(false, false);
        }
    }

    /**
     * Checks whether a systemd service is currently active.
     *
     * @param serviceName the service to check
     * @return true if the service is active
     */
    private boolean isServiceActive(String serviceName) {
        try {
            String output = executeCommand("systemctl is-active " + serviceName);
            return "active".equalsIgnoreCase(output.trim());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Executes a shell command and returns its output.
     *
     * @param command the command to execute
     * @return the command output
     * @throws ApModeException if the command fails or times out
     */
    private String executeCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(
                    AppConstants.COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ApModeException("Command timed out: " + command);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new ApModeException(
                        "Command failed with exit code " + exitCode + ": " + output);
            }

            return output.toString();
        } catch (ApModeException e) {
            throw e;
        } catch (Exception e) {
            throw new ApModeException("Command execution failed: " + e.getMessage(), e);
        }
    }
}
