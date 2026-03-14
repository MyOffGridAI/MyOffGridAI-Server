package com.myoffgridai.system.service;

import com.myoffgridai.common.exception.ApModeException;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.system.model.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Manages the network transition from Access Point mode to home WiFi
 * after the setup wizard completes.
 *
 * <p>Stops hostapd and dnsmasq, starts avahi-daemon for mDNS discovery,
 * and updates SystemConfig. Runs asynchronously so the HTTP response
 * can be delivered before the network changes.</p>
 */
@Service
public class NetworkTransitionService {

    private static final Logger log = LoggerFactory.getLogger(NetworkTransitionService.class);

    private final ApModeService apModeService;
    private final SystemConfigService systemConfigService;
    private final boolean mockMode;

    /**
     * Constructs the network transition service.
     *
     * @param apModeService       the AP mode service
     * @param systemConfigService the system config service
     * @param mockMode            whether to run in mock mode (no OS commands)
     */
    public NetworkTransitionService(ApModeService apModeService,
                                    SystemConfigService systemConfigService,
                                    @Value("${app.ap.mock:true}") boolean mockMode) {
        this.apModeService = apModeService;
        this.systemConfigService = systemConfigService;
        this.mockMode = mockMode;
    }

    /**
     * Finalizes the setup by transitioning from AP mode to home network.
     *
     * <p>Steps performed:</p>
     * <ol>
     *   <li>Stops hostapd and dnsmasq via {@link ApModeService#stopApMode()}</li>
     *   <li>Waits for network stabilization</li>
     *   <li>Starts avahi-daemon for mDNS (offgrid.local)</li>
     *   <li>Updates SystemConfig.apModeEnabled to false</li>
     * </ol>
     *
     * <p>Runs asynchronously so the HTTP response returns before network changes.</p>
     */
    @Async
    public void finalizeSetup() {
        log.info("Finalizing setup — transitioning from AP mode to home network");

        try {
            // Step 1: Stop AP mode
            apModeService.stopApMode();

            // Step 2: Wait for network to stabilize
            Thread.sleep(AppConstants.NETWORK_TRANSITION_DELAY_SECONDS * 1000L);

            // Step 3: Start avahi-daemon for mDNS
            if (!mockMode) {
                startAvahi();
            } else {
                log.warn("Avahi start simulated (mock mode)");
            }

            // Step 4: Update SystemConfig
            SystemConfig config = systemConfigService.getConfig();
            config.setApModeEnabled(false);
            config.setWifiConfigured(true);
            systemConfigService.save(config);

            log.info("Setup complete — device now accessible at offgrid.local");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Setup finalization interrupted", e);
        } catch (Exception e) {
            log.error("Setup finalization failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Starts the avahi-daemon service for mDNS discovery.
     *
     * @throws ApModeException if the command fails
     */
    private void startAvahi() {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", "sudo systemctl start avahi-daemon");
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
                log.warn("Avahi start command timed out");
                return;
            }

            if (process.exitValue() != 0) {
                log.warn("Avahi start returned exit code {}: {}", process.exitValue(), output);
            } else {
                log.info("avahi-daemon started — mDNS broadcasting offgrid.local");
            }
        } catch (Exception e) {
            log.warn("Failed to start avahi-daemon: {}", e.getMessage());
        }
    }
}
