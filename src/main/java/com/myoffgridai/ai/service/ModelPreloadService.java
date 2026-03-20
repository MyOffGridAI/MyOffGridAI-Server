package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.OllamaChatRequest;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Preloads AI models into Ollama on server startup to avoid cold-start latency.
 *
 * <p>Without preloading, the first chat request incurs a 30-60 second delay
 * while Ollama loads the model from disk into GPU memory. This service sends
 * a minimal warm-up request for both the chat and embedding models immediately
 * after the application context is ready.</p>
 */
@Service
public class ModelPreloadService {

    private static final Logger log = LoggerFactory.getLogger(ModelPreloadService.class);

    private final OllamaService ollamaService;
    private final SystemConfigService systemConfigService;
    private OllamaInferenceService ollamaInferenceService;

    /**
     * Constructs the model preload service.
     *
     * @param ollamaService       the Ollama integration service
     * @param systemConfigService the system config service for AI settings
     */
    public ModelPreloadService(OllamaService ollamaService,
                                SystemConfigService systemConfigService) {
        this.ollamaService = ollamaService;
        this.systemConfigService = systemConfigService;
    }

    /**
     * Optionally injects the Ollama inference service for thinking-cache warming.
     *
     * <p>Uses setter injection because {@link OllamaInferenceService} is conditional
     * on {@code app.inference.provider=ollama} and may not be present.</p>
     *
     * @param ollamaInferenceService the Ollama inference service (may be absent)
     */
    @Autowired(required = false)
    public void setOllamaInferenceService(OllamaInferenceService ollamaInferenceService) {
        this.ollamaInferenceService = ollamaInferenceService;
    }

    /**
     * Preloads AI models after the application is fully started.
     *
     * <p>Runs asynchronously to avoid blocking server startup. Sends minimal
     * requests to force Ollama to load both the chat model and the embedding
     * model into memory, with a 24h keep_alive to prevent unloading.</p>
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void preloadModels() {
        log.info("Preloading AI models into Ollama...");

        // Preload embedding model first (smaller, faster)
        preloadEmbeddingModel();

        // Preload chat model (larger, takes longer)
        preloadChatModel();

        log.info("Model preloading complete");
    }

    private void preloadEmbeddingModel() {
        try {
            long start = System.currentTimeMillis();
            ollamaService.embed("warmup");
            long elapsed = System.currentTimeMillis() - start;
            log.info("Embedding model preloaded in {}ms", elapsed);
        } catch (Exception e) {
            log.warn("Failed to preload embedding model: {}", e.getMessage());
        }
    }

    private void preloadChatModel() {
        try {
            long start = System.currentTimeMillis();
            var aiSettings = systemConfigService.getAiSettings();
            String model = aiSettings.modelName() != null
                    ? aiSettings.modelName() : AppConstants.OLLAMA_MODEL;

            OllamaChatRequest request = new OllamaChatRequest(
                    model,
                    List.of(new OllamaMessage("user", "hi")),
                    false,
                    Map.of("num_ctx", aiSettings.contextSize(), "num_predict", 1),
                    false,
                    AppConstants.OLLAMA_KEEP_ALIVE
            );

            ollamaService.chat(request);
            long elapsed = System.currentTimeMillis() - start;
            log.info("Chat model '{}' preloaded in {}ms", model, elapsed);

            // Eagerly warm the thinking-capability cache so the first chat
            // request does not incur a /api/show round-trip.
            if (ollamaInferenceService != null) {
                ollamaInferenceService.warmThinkingCache(model);
            }
        } catch (Exception e) {
            log.warn("Failed to preload chat model: {}", e.getMessage());
        }
    }
}
