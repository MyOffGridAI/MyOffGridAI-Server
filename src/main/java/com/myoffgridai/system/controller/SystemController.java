package com.myoffgridai.system.controller;

import com.myoffgridai.auth.dto.AuthResponse;
import com.myoffgridai.auth.dto.RegisterRequest;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.system.dto.FactoryResetRequest;
import com.myoffgridai.system.dto.InitializeRequest;
import com.myoffgridai.system.dto.SystemStatusDto;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.service.FactoryResetService;
import com.myoffgridai.system.service.NetworkTransitionService;
import com.myoffgridai.system.service.SystemConfigService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final NetworkTransitionService networkTransitionService;
    private final FactoryResetService factoryResetService;

    /**
     * Constructs the system controller.
     *
     * @param systemConfigService      the system config service
     * @param authService              the auth service for creating the owner account
     * @param networkTransitionService the network transition service
     * @param factoryResetService      the factory reset service
     */
    public SystemController(SystemConfigService systemConfigService,
                            AuthService authService,
                            NetworkTransitionService networkTransitionService,
                            FactoryResetService factoryResetService) {
        this.systemConfigService = systemConfigService;
        this.authService = authService;
        this.networkTransitionService = networkTransitionService;
        this.factoryResetService = factoryResetService;
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

    /**
     * Finalizes the setup by transitioning from AP mode to home network.
     * Public endpoint — called from the setup wizard confirm page before
     * the user has a saved token in the browser.
     *
     * <p>Runs asynchronously so the response returns before network changes.
     * Only executes if the system has been initialized.</p>
     *
     * @return success message
     */
    @PostMapping("/finalize-setup")
    public ResponseEntity<ApiResponse<String>> finalizeSetup() {
        if (!systemConfigService.isInitialized()) {
            log.warn("Finalize-setup called before initialization");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("System must be initialized before finalizing setup"));
        }

        log.info("Finalize-setup requested — transitioning to home network");
        networkTransitionService.finalizeSetup();
        return ResponseEntity.ok(
                ApiResponse.success("Setup finalized — transitioning to home network"));
    }

    /**
     * Performs a factory reset, returning the device to its initial setup state.
     * Only accessible by OWNER role. Requires the confirmation phrase
     * "RESET MY DEVICE" to prevent accidental invocations.
     *
     * @param request the factory reset request with confirmation phrase
     * @return success message (server may restart, so response may not arrive)
     */
    @PostMapping("/factory-reset")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> factoryReset(
            @Valid @RequestBody FactoryResetRequest request) {
        if (!AppConstants.FACTORY_RESET_CONFIRM_PHRASE.equals(request.confirmPhrase())) {
            log.warn("Factory reset attempted with wrong confirmation phrase");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            "Invalid confirmation phrase. Must be: "
                                    + AppConstants.FACTORY_RESET_CONFIRM_PHRASE));
        }

        log.warn("Factory reset initiated by authenticated user");
        factoryResetService.performReset();
        return ResponseEntity.ok(
                ApiResponse.success(null, "Factory reset in progress — device returning to setup mode"));
    }
}
