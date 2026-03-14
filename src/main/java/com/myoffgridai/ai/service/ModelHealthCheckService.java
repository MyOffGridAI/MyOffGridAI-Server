package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.OllamaModelInfo;
import com.myoffgridai.config.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Performs a health check on the Ollama LLM service at application startup.
 *
 * <p>Logs a warning if Ollama is unavailable (expected in CI/test environments)
 * and verifies the configured model is available. Does not prevent server startup.</p>
 */
@Component
public class ModelHealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(ModelHealthCheckService.class);

    private final OllamaService ollamaService;

    /**
     * Constructs the health check service.
     *
     * @param ollamaService the Ollama integration service
     */
    public ModelHealthCheckService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    /**
     * Checks Ollama availability when the application is ready.
     *
     * <p>If Ollama is available, lists loaded models and verifies the configured
     * model is present. If unavailable, logs a warning but does not throw.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkOllamaOnStartup() {
        log.info("Checking Ollama availability at startup...");

        if (!ollamaService.isAvailable()) {
            log.warn("Ollama not available at startup. Chat features will be unavailable "
                    + "until Ollama is running at {}. This is expected in CI/test environments.",
                    AppConstants.OLLAMA_BASE_URL);
            return;
        }

        log.info("Ollama is available at {}", AppConstants.OLLAMA_BASE_URL);

        try {
            List<OllamaModelInfo> models = ollamaService.listModels();
            log.info("Available Ollama models: {}", models.stream()
                    .map(OllamaModelInfo::name)
                    .toList());

            boolean configuredModelFound = models.stream()
                    .anyMatch(m -> m.name().equals(AppConstants.OLLAMA_MODEL));

            if (!configuredModelFound) {
                log.warn("Configured model '{}' not found in available models. "
                        + "Run 'ollama pull {}' to download it.",
                        AppConstants.OLLAMA_MODEL, AppConstants.OLLAMA_MODEL);
            }
        } catch (Exception e) {
            log.warn("Failed to list Ollama models: {}", e.getMessage());
        }
    }
}
