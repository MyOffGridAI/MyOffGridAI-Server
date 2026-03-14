package com.myoffgridai.system.controller;

import com.myoffgridai.auth.dto.AuthResponse;
import com.myoffgridai.auth.dto.RegisterRequest;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.system.dto.InitializeRequest;
import com.myoffgridai.system.dto.SystemStatusDto;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.service.SystemConfigService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST controller for system management and first-boot initialization.
 *
 * <p>The {@code /api/system/status} and {@code /api/system/initialize} endpoints
 * are public (no authentication required) to support the first-boot setup flow
 * before any users exist in the system.</p>
 */
@RestController
@RequestMapping(AppConstants.API_SYSTEM)
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);

    private static final String SERVER_VERSION = "1.0.0";

    private final SystemConfigService systemConfigService;
    private final AuthService authService;

    /**
     * Constructs the system controller.
     *
     * @param systemConfigService the system config service
     * @param authService         the auth service for creating the owner account
     */
    public SystemController(SystemConfigService systemConfigService,
                            AuthService authService) {
        this.systemConfigService = systemConfigService;
        this.authService = authService;
    }

    /**
     * Returns the current system status. Public endpoint — no auth required.
     *
     * @return the system status including initialization state and fortress mode
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SystemStatusDto>> getStatus() {
        SystemConfig config = systemConfigService.getConfig();
        SystemStatusDto status = new SystemStatusDto(
                config.isInitialized(),
                config.getInstanceName(),
                config.isFortressEnabled(),
                config.isWifiConfigured(),
                SERVER_VERSION,
                Instant.now()
        );
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Initializes the system on first boot. Creates the OWNER account and
     * marks the system as initialized. Public endpoint — no auth required.
     *
     * <p>This endpoint can only be called once. Subsequent calls return 409 Conflict.</p>
     *
     * @param request the initialization request with instance name and owner credentials
     * @return the auth response with the owner's JWT tokens
     */
    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<AuthResponse>> initialize(@Valid @RequestBody InitializeRequest request) {
        if (systemConfigService.isInitialized()) {
            log.warn("System already initialized, rejecting re-initialization attempt");
            return ResponseEntity.status(409)
                    .body(ApiResponse.error("System is already initialized"));
        }

        log.info("Initializing system with instance name: {}", request.instanceName());

        // Create the OWNER account
        RegisterRequest registerRequest = new RegisterRequest(
                request.username(),
                request.email(),
                request.displayName(),
                request.password(),
                Role.ROLE_OWNER
        );
        AuthResponse authResponse = authService.register(registerRequest);

        // Mark system as initialized
        systemConfigService.setInitialized(request.instanceName());

        log.info("System initialized successfully. Owner account created: {}", request.username());
        return ResponseEntity.ok(ApiResponse.success(authResponse, "System initialized successfully"));
    }
}
