package com.myoffgridai.knowledge.service;

import com.myoffgridai.config.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Checks that the knowledge storage directory exists and is writable at startup.
 *
 * <p>Logs a warning if the directory does not exist or is not writable,
 * but does not prevent application startup.</p>
 */
@Service
public class StorageHealthService {

    private static final Logger log = LoggerFactory.getLogger(StorageHealthService.class);

    /**
     * Verifies the knowledge storage directory on application startup.
     * Creates the directory if it does not exist.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkStorageDirectory() {
        Path storagePath = Paths.get(AppConstants.KNOWLEDGE_STORAGE_BASE_PATH);
        if (!Files.exists(storagePath)) {
            try {
                Files.createDirectories(storagePath);
                log.info("Created knowledge storage directory: {}", storagePath);
            } catch (IOException e) {
                log.warn("Cannot create knowledge storage directory: {}. "
                        + "File uploads will fail until this is resolved.", storagePath);
            }
        } else if (!Files.isWritable(storagePath)) {
            log.warn("Knowledge storage directory is not writable: {}. "
                    + "File uploads will fail until this is resolved.", storagePath);
        } else {
            log.info("Knowledge storage directory OK: {}", storagePath);
        }
    }
}
