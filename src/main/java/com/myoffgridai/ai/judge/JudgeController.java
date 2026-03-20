package com.myoffgridai.ai.judge;

import com.myoffgridai.ai.dto.OllamaChatRequest;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.settings.dto.ExternalApiSettingsDto;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import com.myoffgridai.system.service.SystemConfigService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for AI judge model management and testing.
 *
 * <p>Provides endpoints to start/stop the judge llama-server process,
 * query its status, and test judge evaluation. If no assistant response
 * is provided for testing, one is generated from the local LLM first.</p>
 */
@RestController
@RequestMapping("/api/ai/judge")
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class JudgeController {

    private static final Logger log = LoggerFactory.getLogger(JudgeController.class);

    private final JudgeModelProcessService judgeModelProcessService;
    private final JudgeInferenceService judgeInferenceService;
    private final ExternalApiSettingsService externalApiSettingsService;
    private final OllamaService ollamaService;
    private final SystemConfigService systemConfigService;

    /**
     * Constructs the judge controller.
     *
     * @param judgeModelProcessService  the judge process manager
     * @param judgeInferenceService     the judge inference service
     * @param externalApiSettingsService the external API settings service
     * @param ollamaService             the Ollama service for generating responses
     * @param systemConfigService       the system config service for AI settings
     */
    public JudgeController(JudgeModelProcessService judgeModelProcessService,
                            JudgeInferenceService judgeInferenceService,
                            ExternalApiSettingsService externalApiSettingsService,
                            OllamaService ollamaService,
                            SystemConfigService systemConfigService) {
        this.judgeModelProcessService = judgeModelProcessService;
        this.judgeInferenceService = judgeInferenceService;
        this.externalApiSettingsService = externalApiSettingsService;
        this.ollamaService = ollamaService;
        this.systemConfigService = systemConfigService;
    }

    /**
     * Returns the current judge subsystem status.
     *
     * @return the judge status DTO
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<JudgeStatusDto>> getStatus() {
        log.debug("Getting judge status");
        return ResponseEntity.ok(ApiResponse.success(buildStatusDto()));
    }

    /**
     * Starts the judge llama-server process if not already running.
     *
     * @return the updated judge status DTO
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<JudgeStatusDto>> start() {
        log.info("Starting judge process");
        judgeModelProcessService.start();
        return ResponseEntity.ok(ApiResponse.success(buildStatusDto()));
    }

    /**
     * Stops the judge llama-server process if running.
     *
     * @return the updated judge status DTO
     */
    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<JudgeStatusDto>> stop() {
        log.info("Stopping judge process");
        judgeModelProcessService.stop();
        return ResponseEntity.ok(ApiResponse.success(buildStatusDto()));
    }

    /**
     * Tests the judge evaluation pipeline.
     *
     * <p>If no assistant response is provided, generates one from the local
     * LLM (Ollama) first, then sends both the query and response to the
     * judge for scoring.</p>
     *
     * @param request the test request containing the query and optional response
     * @return the judge test result including the evaluated response
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<JudgeTestResultDto>> test(
            @Valid @RequestBody JudgeTestRequest request) {
        log.info("Testing judge evaluation");

        if (!judgeInferenceService.isAvailable()) {
            return ResponseEntity.ok(ApiResponse.success(
                    new JudgeTestResultDto(null, 0.0, null, false, false,
                            "Judge is not available")));
        }

        try {
            // Generate a response from the local LLM if none provided
            String assistantResponse = request.response();
            if (assistantResponse == null || assistantResponse.isBlank()) {
                assistantResponse = generateResponse(request.query());
            }

            Optional<JudgeResult> result = judgeInferenceService.evaluate(
                    request.query(), assistantResponse);

            if (result.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(
                        new JudgeTestResultDto(
                                assistantResponse,
                                result.get().score(),
                                result.get().reason(),
                                result.get().needsCloud(),
                                true,
                                null)));
            } else {
                return ResponseEntity.ok(ApiResponse.success(
                        new JudgeTestResultDto(assistantResponse, 0.0, null, false, true,
                                "Judge returned empty result — could not parse response")));
            }

        } catch (Exception e) {
            log.error("Judge test failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(
                    new JudgeTestResultDto(null, 0.0, null, false, true, e.getMessage())));
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private String generateResponse(String query) {
        var aiSettings = systemConfigService.getAiSettings();
        String model = aiSettings.modelName() != null
                ? aiSettings.modelName() : AppConstants.OLLAMA_MODEL;

        OllamaChatRequest chatRequest = new OllamaChatRequest(
                model,
                List.of(new OllamaMessage("user", query)),
                false,
                Map.of("num_ctx", aiSettings.contextSize(), "num_predict", 256),
                false,
                AppConstants.OLLAMA_KEEP_ALIVE
        );

        OllamaChatResponse chatResponse = ollamaService.chat(chatRequest);
        return chatResponse.message().content();
    }

    private JudgeStatusDto buildStatusDto() {
        ExternalApiSettingsDto settings = externalApiSettingsService.getSettings();
        return new JudgeStatusDto(
                settings.judgeEnabled(),
                judgeModelProcessService.isRunning(),
                settings.judgeModelFilename(),
                judgeModelProcessService.getPort(),
                settings.judgeScoreThreshold()
        );
    }
}
