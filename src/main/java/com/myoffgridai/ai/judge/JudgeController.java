package com.myoffgridai.ai.judge;

import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.settings.dto.ExternalApiSettingsDto;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for AI judge model management and testing.
 *
 * <p>Provides endpoints to start/stop the judge llama-server process,
 * query its status, and test judge evaluation against arbitrary
 * query/response pairs. All endpoints require ADMIN or OWNER role.</p>
 */
@RestController
@RequestMapping("/api/ai/judge")
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class JudgeController {

    private static final Logger log = LoggerFactory.getLogger(JudgeController.class);

    private final JudgeModelProcessService judgeModelProcessService;
    private final JudgeInferenceService judgeInferenceService;
    private final ExternalApiSettingsService externalApiSettingsService;

    /**
     * Constructs the judge controller.
     *
     * @param judgeModelProcessService the judge process manager
     * @param judgeInferenceService    the judge inference service
     * @param externalApiSettingsService the external API settings service
     */
    public JudgeController(JudgeModelProcessService judgeModelProcessService,
                            JudgeInferenceService judgeInferenceService,
                            ExternalApiSettingsService externalApiSettingsService) {
        this.judgeModelProcessService = judgeModelProcessService;
        this.judgeInferenceService = judgeInferenceService;
        this.externalApiSettingsService = externalApiSettingsService;
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
     * Tests the judge evaluation against a provided query/response pair.
     *
     * <p>If the judge is unavailable, returns a result with
     * {@code judgeAvailable=false}.</p>
     *
     * @param request the test request containing query and response
     * @return the judge test result DTO
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<JudgeTestResultDto>> test(
            @Valid @RequestBody JudgeTestRequest request) {
        log.info("Testing judge evaluation");

        if (!judgeInferenceService.isAvailable()) {
            return ResponseEntity.ok(ApiResponse.success(
                    new JudgeTestResultDto(0.0, null, false, false, "Judge is not available")));
        }

        try {
            Optional<JudgeResult> result = judgeInferenceService.evaluate(
                    request.query(), request.response());

            if (result.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(
                        new JudgeTestResultDto(
                                result.get().score(),
                                result.get().reason(),
                                result.get().needsCloud(),
                                true,
                                null)));
            } else {
                return ResponseEntity.ok(ApiResponse.success(
                        new JudgeTestResultDto(0.0, null, false, true,
                                "Judge returned empty result — could not parse response")));
            }

        } catch (Exception e) {
            log.error("Judge test failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(
                    new JudgeTestResultDto(0.0, null, false, true, e.getMessage())));
        }
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
