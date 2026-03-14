package com.myoffgridai.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Startup listener that checks whether the system has been initialized
 * and starts or stops Access Point mode accordingly.
 *
 * <p>Runs after all other startup listeners via {@link Order} to ensure
 * all Spring beans are fully initialized before performing AP mode operations.</p>
 */
@Component
public class ApModeStartupService {

    private static final Logger log = LoggerFactory.getLogger(ApModeStartupService.class);

    private final SystemConfigService systemConfigService;
    private final ApModeService apModeService;

    /**
     * Constructs the AP mode startup service.
     *
     * @param systemConfigService the system config service
     * @param apModeService       the AP mode service
     */
    public ApModeStartupService(SystemConfigService systemConfigService,
                                ApModeService apModeService) {
        this.systemConfigService = systemConfigService;
        this.apModeService = apModeService;
    }

    /**
     * Checks system initialization state on application startup and starts
     * or stops AP mode accordingly.
     *
     * <p>If the system is not initialized, starts AP mode and sets the
     * {@code apModeEnabled} flag. If already initialized, ensures AP mode
     * is stopped.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order
    public void onApplicationReady() {
        boolean initialized = systemConfigService.isInitialized();

        if (!initialized) {
            log.info("System not initialized — starting AP mode for setup wizard");
            apModeService.startApMode();
            var config = systemConfigService.getConfig();
            config.setApModeEnabled(true);
            systemConfigService.save(config);
            log.info("AP mode enabled, broadcasting SSID for captive portal");
        } else {
            log.info("System already initialized — ensuring AP mode is stopped");
            apModeService.stopApMode();
            log.info("System state: initialized=true, AP mode stopped");
        }
    }
}
