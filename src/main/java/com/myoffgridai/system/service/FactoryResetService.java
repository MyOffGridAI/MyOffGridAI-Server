package com.myoffgridai.system.service;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.system.model.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Performs factory reset operations, returning the device to its
 * initial setup state.
 *
 * <p>A factory reset clears the device initialization state but does NOT
 * wipe user data. The device returns to AP mode so the setup wizard can
 * be re-run.</p>
 */
@Service
public class FactoryResetService {

    private static final Logger log = LoggerFactory.getLogger(FactoryResetService.class);

    private final SystemConfigService systemConfigService;
    private final ApModeService apModeService;

    /**
     * Constructs the factory reset service.
     *
     * @param systemConfigService the system config service
     * @param apModeService       the AP mode service
     */
    public FactoryResetService(SystemConfigService systemConfigService,
                               ApModeService apModeService) {
        this.systemConfigService = systemConfigService;
        this.apModeService = apModeService;
    }

    /**
     * Performs a factory reset via API request.
     *
     * <p>Runs asynchronously so the HTTP response can be delivered before
     * the reset takes effect. Waits {@link AppConstants#FACTORY_RESET_DELAY_SECONDS}
     * before executing to allow response delivery.</p>
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Waits for response delivery</li>
     *   <li>Resets SystemConfig to defaults</li>
     *   <li>Re-enables AP mode for setup wizard</li>
     * </ol>
     */
    @Async
    public void performReset() {
        log.warn("Factory reset initiated — waiting for response delivery");
        try {
            Thread.sleep(AppConstants.FACTORY_RESET_DELAY_SECONDS * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        resetSystemConfig();
        restartApMode();
        log.warn("Factory reset complete — device returning to setup mode");
    }

    /**
     * Performs a factory reset triggered by USB file detection.
     *
     * <p>Does not require authentication. Executes immediately without
     * delay since there is no HTTP response to deliver.</p>
     */
    public void performUsbReset() {
        log.warn("USB factory reset triggered — performing reset");
        resetSystemConfig();
        restartApMode();
        log.warn("USB factory reset complete — device returning to setup mode");
    }

    /**
     * Resets the SystemConfig to its default (uninitialized) state.
     */
    private void resetSystemConfig() {
        SystemConfig config = systemConfigService.getConfig();
        config.setInitialized(false);
        config.setInstanceName(null);
        config.setApModeEnabled(false);
        config.setFortressEnabled(false);
        config.setFortressEnabledAt(null);
        config.setFortressEnabledByUserId(null);
        config.setWifiConfigured(false);
        systemConfigService.save(config);
        log.info("SystemConfig reset to defaults");
    }

    /**
     * Starts AP mode so the setup wizard becomes accessible.
     */
    private void restartApMode() {
        try {
            apModeService.startApMode();
            SystemConfig config = systemConfigService.getConfig();
            config.setApModeEnabled(true);
            systemConfigService.save(config);
        } catch (Exception e) {
            log.error("Failed to restart AP mode after factory reset: {}", e.getMessage());
        }
    }
}
