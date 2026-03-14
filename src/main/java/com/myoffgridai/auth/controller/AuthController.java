package com.myoffgridai.auth.controller;

import com.myoffgridai.auth.dto.*;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations including registration,
 * login, token refresh, and logout.
 *
 * <p>All endpoints return responses wrapped in {@link ApiResponse}.
 * Sensitive data (passwords) is never logged.</p>
 */
@RestController
@RequestMapping(AppConstants.API_AUTH)
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    /**
     * Constructs the controller with the authentication service.
     *
     * @param authService the authentication service
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user account.
     *
     * <p>Public endpoint — allowed when no OWNER exists or caller is OWNER/ADMIN.
     * Registration logic is enforced in the service layer.</p>
     *
     * @param request the registration request
     * @return 201 Created with JWT token pair
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for username: {}", request.username());
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authResponse, "User registered successfully"));
    }

    /**
     * Authenticates a user with username and password.
     *
     * @param request the login credentials
     * @return 200 OK with JWT token pair
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for username: {}", request.username());
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    /**
     * Issues a new access token using a valid refresh token.
     *
     * @param request the refresh token request
     * @return 200 OK with new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        log.debug("Token refresh request received");
        AuthResponse authResponse = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed"));
    }

    /**
     * Invalidates the current access token (logout).
     *
     * @param authHeader the Authorization header containing the Bearer token
     * @return 200 OK confirming logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(AppConstants.AUTHORIZATION_HEADER) String authHeader) {
        log.info("Logout request received");
        String token = authHeader.substring(AppConstants.BEARER_PREFIX.length());
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }
}
