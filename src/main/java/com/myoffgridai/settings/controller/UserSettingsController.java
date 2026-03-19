package com.myoffgridai.settings.controller;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.settings.dto.UpdateUserSettingsRequest;
import com.myoffgridai.settings.dto.UserSettingsDto;
import com.myoffgridai.settings.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing per-user application settings.
 *
 * <p>Accessible to any authenticated user. Each user can only read and
 * update their own settings.</p>
 */
@RestController
@RequestMapping(AppConstants.API_USER_SETTINGS)
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    /**
     * Constructs the controller.
     *
     * @param userSettingsService the user settings service
     */
    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    /**
     * Returns the current user's settings.
     *
     * @param principal the authenticated user
     * @return the settings DTO
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserSettingsDto>> getSettings(
            @AuthenticationPrincipal User principal) {
        UserSettingsDto settings = userSettingsService.getSettings(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    /**
     * Updates the current user's settings.
     *
     * @param principal the authenticated user
     * @param request   the update request
     * @return the updated settings DTO
     */
    @PutMapping
    public ResponseEntity<ApiResponse<UserSettingsDto>> updateSettings(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody UpdateUserSettingsRequest request) {
        UserSettingsDto settings = userSettingsService.updateSettings(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(settings, "User settings updated"));
    }
}
