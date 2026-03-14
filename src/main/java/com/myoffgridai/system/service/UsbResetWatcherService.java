package com.myoffgridai.system.service;

import com.myoffgridai.config.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Watches for USB-based factory reset and update trigger files.
 *
 * <p>Checks every 30 seconds for:</p>
 * <ul>
 *   <li>{@code factory-reset.trigger} — triggers a full factory reset</li>
 *   <li>{@code myoffgridai-update.zip} — triggers an update (stubbed for MI-002)</li>
 * </ul>
 */
@Component
public class UsbResetWatcherService {

    private static final Logger log = LoggerFactory.getLogger(UsbResetWatcherService.class);

    private final FactoryResetService factoryResetService;

    /**
     * Constructs the USB reset watcher service.
     *
     * @param factoryResetService the factory reset service
     */
    public UsbResetWatcherService(FactoryResetService factoryResetService) {
        this.factoryResetService = factoryResetService;
    }

    /**
     * Periodically checks the USB mount path for trigger files.
     *
     * <p>If a factory reset trigger file is found, it is deleted immediately
     * to prevent re-triggering, then a USB factory reset is performed.</p>
     *
     * <p>If an update zip is found, the update service is called (stubbed).</p>
     */
    @Scheduled(fixedDelay = 30000)
    public void checkForTriggerFiles() {
        Path usbPath = Paths.get(AppConstants.USB_MOUNT_PATH);

        if (!Files.isDirectory(usbPath)) {
            return;
        }

        checkFactoryResetTrigger(usbPath);
        checkUpdateZip(usbPath);
    }

    /**
     * Checks for the factory reset trigger file and performs reset if found.
     *
     * @param usbPath the USB mount directory path
     */
    private void checkFactoryResetTrigger(Path usbPath) {
        Path triggerFile = usbPath.resolve(AppConstants.FACTORY_RESET_TRIGGER_FILENAME);

        if (Files.exists(triggerFile)) {
            log.warn("Factory reset trigger file detected on USB — performing reset");

            try {
                Files.delete(triggerFile);
                log.info("Factory reset trigger file deleted");
            } catch (IOException e) {
                log.error("Failed to delete factory reset trigger file: {}", e.getMessage());
            }

            factoryResetService.performUsbReset();
        }
    }

    /**
     * Checks for the update zip file and invokes the update service if found.
     * Update application is deferred to MI-002 — this is a stub.
     *
     * @param usbPath the USB mount directory path
     */
    private void checkUpdateZip(Path usbPath) {
        Path updateFile = usbPath.resolve(AppConstants.UPDATE_ZIP_FILENAME);

        if (Files.exists(updateFile)) {
            log.info("Update zip detected on USB: {} — update application deferred to MI-002",
                    updateFile);
            // TODO: UpdateService.applyUpdate() — deferred to MI-002
        }
    }
}
