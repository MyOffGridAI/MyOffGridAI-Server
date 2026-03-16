package com.myoffgridai.settings.controller;

import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.settings.dto.ExternalApiSettingsDto;
import com.myoffgridai.settings.dto.UpdateExternalApiSettingsRequest;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing external API settings (Anthropic, Brave Search).
 *
 * <p>Restricted to OWNER role only. API keys are never returned in responses.</p>
 */
@RestController
@RequestMapping("/api/settings/external-apis")
public class ExternalApiSettingsController {

    private final ExternalApiSettingsService settingsService;

    /**
     * Constructs the controller.
     *
     * @param settingsService the external API settings service
     */
    public ExternalApiSettingsController(ExternalApiSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Returns the current external API settings.
     *
     * @return the settings DTO (keys masked as boolean flags)
     */
    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<ExternalApiSettingsDto>> getSettings() {
        ExternalApiSettingsDto settings = settingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    /**
     * Updates external API settings.
     *
     * @param request the update request
     * @return the updated settings DTO
     */
    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<ExternalApiSettingsDto>> updateSettings(
            @Valid @RequestBody UpdateExternalApiSettingsRequest request) {
        ExternalApiSettingsDto settings = settingsService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.success(settings, "External API settings updated"));
    }
}
