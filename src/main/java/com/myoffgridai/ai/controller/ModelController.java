package com.myoffgridai.ai.controller;

import com.myoffgridai.ai.dto.ActiveModelDto;
import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.ai.dto.OllamaHealthDto;
import com.myoffgridai.ai.dto.OllamaModelInfo;
import com.myoffgridai.ai.service.InferenceService;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for inference model management and health checking.
 *
 * <p>Provides public endpoints for model listing and health. Delegates to
 * the active {@link InferenceService} implementation (LM Studio or Ollama).</p>
 */
@RestController
@RequestMapping(AppConstants.MODELS_API_PATH)
public class ModelController {

    private static final Logger log = LoggerFactory.getLogger(ModelController.class);

    private final InferenceService inferenceService;
    private final SystemConfigService systemConfigService;

    /**
     * Constructs the model controller.
     *
     * @param inferenceService    the active inference service implementation
     * @param systemConfigService the system config service for dynamic AI settings
     */
    public ModelController(InferenceService inferenceService,
                           SystemConfigService systemConfigService) {
        this.inferenceService = inferenceService;
        this.systemConfigService = systemConfigService;
    }

    /**
     * Lists all available models from the active inference provider. Public endpoint.
     *
     * <p>Returns {@link OllamaModelInfo} DTOs for client backward compatibility.
     * The underlying data comes from {@link InferenceService#listModels()}.</p>
     *
     * @return list of model information
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OllamaModelInfo>>> listModels() {
        log.debug("Listing available models from inference provider");
        List<InferenceModelInfo> models = inferenceService.listModels();

        // Map to OllamaModelInfo for backward client compatibility
        List<OllamaModelInfo> compatModels = models.stream()
                .map(m -> new OllamaModelInfo(
                        m.name(),
                        m.sizeBytes() != null ? m.sizeBytes() : 0L,
                        m.modifiedAt()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(compatModels));
    }

    /**
     * Returns the currently active model info. Requires authentication.
     *
     * @return the active model and embed model names
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<ActiveModelDto>> getActiveModel() {
        log.debug("Getting active model configuration");
        InferenceModelInfo active = inferenceService.getActiveModel();
        ActiveModelDto dto = new ActiveModelDto(
                active.name(),
                AppConstants.OLLAMA_EMBED_MODEL);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * Returns inference provider availability and health status. Public endpoint.
     *
     * @return health status with availability, active model, and response time
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<OllamaHealthDto>> getHealth() {
        log.debug("Checking inference provider health");
        long start = System.currentTimeMillis();
        boolean available = inferenceService.isAvailable();
        long responseTime = System.currentTimeMillis() - start;

        InferenceModelInfo active = inferenceService.getActiveModel();
        OllamaHealthDto dto = new OllamaHealthDto(
                available,
                active.name(),
                AppConstants.OLLAMA_EMBED_MODEL,
                responseTime);

        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
