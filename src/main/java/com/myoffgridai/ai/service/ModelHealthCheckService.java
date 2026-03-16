package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Performs a health check on the inference provider at application startup.
 *
 * <p>Logs a warning if the provider is unavailable (expected in CI/test environments)
 * and verifies the configured model is available. Does not prevent server startup.</p>
 */
@Component
public class ModelHealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(ModelHealthCheckService.class);

    private final InferenceService inferenceService;
    private final SystemConfigService systemConfigService;

    /**
     * Constructs the health check service.
     *
     * @param inferenceService    the active inference service implementation
     * @param systemConfigService the system config service for dynamic AI settings
     */
    public ModelHealthCheckService(InferenceService inferenceService,
                                    SystemConfigService systemConfigService) {
        this.inferenceService = inferenceService;
        this.systemConfigService = systemConfigService;
    }

    /**
     * Checks inference provider availability when the application is ready.
     *
     * <p>If the provider is available, lists loaded models. If unavailable,
     * logs a warning but does not throw.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkInferenceProviderOnStartup() {
        log.info("Checking inference provider availability at startup...");

        if (!inferenceService.isAvailable()) {
            log.warn("Inference provider not available at startup. Chat features will be unavailable "
                    + "until the provider is running. This is expected in CI/test environments.");
            return;
        }

        log.info("Inference provider is available");

        try {
            List<InferenceModelInfo> models = inferenceService.listModels();
            log.info("Available models: {}", models.stream()
                    .map(InferenceModelInfo::name)
                    .toList());
        } catch (Exception e) {
            log.warn("Failed to list models: {}", e.getMessage());
        }
    }
}
