package com.myoffgridai.ai.controller;

import com.myoffgridai.ai.dto.ActiveModelDto;
import com.myoffgridai.ai.dto.OllamaHealthDto;
import com.myoffgridai.ai.dto.OllamaModelInfo;
import com.myoffgridai.ai.service.OllamaService;
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
 * REST controller for Ollama model management and health checking.
 *
 * <p>Provides public endpoints for model listing and health,
 * and an authenticated endpoint for the active model configuration.</p>
 */
@RestController
@RequestMapping(AppConstants.MODELS_API_PATH)
public class ModelController {

    private static final Logger log = LoggerFactory.getLogger(ModelController.class);

    private final OllamaService ollamaService;
    private final SystemConfigService systemConfigService;

    /**
     * Constructs the model controller.
     *
     * @param ollamaService       the Ollama integration service
     * @param systemConfigService the system config service for dynamic AI settings
     */
    public ModelController(OllamaService ollamaService,
                           SystemConfigService systemConfigService) {
        this.ollamaService = ollamaService;
        this.systemConfigService = systemConfigService;
    }

    /**
     * Lists all available Ollama models. Public endpoint.
     *
     * @return list of model information
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OllamaModelInfo>>> listModels() {
        log.debug("Listing available models");
        List<OllamaModelInfo> models = ollamaService.listModels();
        return ResponseEntity.ok(ApiResponse.success(models));
    }

    /**
     * Returns the currently configured active model. Requires authentication.
     *
     * @return the active model and embed model names
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<ActiveModelDto>> getActiveModel() {
        log.debug("Getting active model configuration");
        ActiveModelDto dto = new ActiveModelDto(
                systemConfigService.getAiSettings().modelName(),
                AppConstants.OLLAMA_EMBED_MODEL);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * Returns Ollama availability and health status. Public endpoint.
     *
     * @return health status with availability, active model, and response time
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<OllamaHealthDto>> getHealth() {
        log.debug("Checking Ollama health");
        long start = System.currentTimeMillis();
        boolean available = ollamaService.isAvailable();
        long responseTime = System.currentTimeMillis() - start;

        OllamaHealthDto dto = new OllamaHealthDto(
                available,
                systemConfigService.getAiSettings().modelName(),
                AppConstants.OLLAMA_EMBED_MODEL,
                responseTime);

        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
