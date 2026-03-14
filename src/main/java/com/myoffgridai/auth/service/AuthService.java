package com.myoffgridai.auth.service;

import com.myoffgridai.auth.dto.*;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.common.exception.DuplicateResourceException;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.config.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service handling user authentication, registration, token lifecycle,
 * and password management.
 *
 * <p>Delegates JWT operations to {@link JwtService} and password hashing
 * to Spring Security's {@link PasswordEncoder}. Token blacklisting uses
 * an in-memory concurrent set in dev profile.</p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();
    private final int minPasswordLength;

    /**
     * Constructs the AuthService with required dependencies.
     *
     * @param userRepository  the user data access layer
     * @param jwtService      the JWT generation/validation service
     * @param passwordEncoder the password hashing encoder
     * @param profile         the active Spring profile (dev or prod)
     */
    public AuthService(UserRepository userRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       @Value("${spring.profiles.active:dev}") String profile) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.minPasswordLength = "prod".equals(profile)
                ? AppConstants.PASSWORD_MIN_LENGTH_PROD
                : AppConstants.PASSWORD_MIN_LENGTH_DEV;
    }

    /**
     * Registers a new user account.
     *
     * <p>Validates username/email uniqueness, enforces password policy,
     * hashes the password with BCrypt, persists the user, and returns
     * a JWT token pair.</p>
     *
     * @param request the registration request containing user details
     * @return an {@link AuthResponse} with access and refresh tokens
     * @throws DuplicateResourceException if username or email already exists
     * @throws IllegalArgumentException   if the password is too short
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.username());

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }

        if (request.email() != null && !request.email().isBlank()
                && userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
        }

        validatePassword(request.password());

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setDisplayName(request.displayName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role() != null ? request.role() : Role.ROLE_MEMBER);
        user.setIsActive(true);

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());

        return buildAuthResponse(user);
    }

    /**
     * Authenticates a user with username and password.
     *
     * <p>Verifies credentials, updates the last login timestamp,
     * and returns a JWT token pair.</p>
     *
     * @param request the login request containing credentials
     * @return an {@link AuthResponse} with access and refresh tokens
     * @throws BadCredentialsException if credentials are invalid
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.username());

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.getIsActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in successfully: {}", user.getUsername());
        return buildAuthResponse(user);
    }

    /**
     * Issues a new access token using a valid refresh token.
     *
     * @param refreshToken the refresh token to validate
     * @return an {@link AuthResponse} with a new access token and the same refresh token
     * @throws BadCredentialsException if the refresh token is invalid or expired
     */
    public AuthResponse refresh(String refreshToken) {
        log.debug("Refreshing access token");

        if (tokenBlacklist.contains(refreshToken)) {
            throw new BadCredentialsException("Token has been revoked");
        }

        String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        if (jwtService.isTokenExpired(refreshToken)) {
            throw new BadCredentialsException("Refresh token has expired");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(user);

        return new AuthResponse(
                newAccessToken,
                refreshToken,
                AppConstants.TOKEN_TYPE_BEARER,
                jwtService.getAccessExpirationMs() / 1000,
                toUserSummary(user)
        );
    }

    /**
     * Invalidates a token by adding it to the blacklist.
     *
     * @param token the token to invalidate
     */
    public void logout(String token) {
        log.info("Logging out — blacklisting token");
        tokenBlacklist.add(token);
    }

    /**
     * Checks whether a token has been blacklisted via logout.
     *
     * @param token the token to check
     * @return {@code true} if the token has been revoked
     */
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.contains(token);
    }

    /**
     * Changes a user's password after verifying the current password.
     *
     * @param userId  the ID of the user changing their password
     * @param request the request containing current and new passwords
     * @throws EntityNotFoundException if the user is not found
     * @throws BadCredentialsException if the current password is incorrect
     * @throws IllegalArgumentException if the new password is too short
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("Password change requested for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        validatePassword(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getUsername());
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void validatePassword(String password) {
        if (password == null || password.length() < minPasswordLength) {
            throw new IllegalArgumentException(
                    "Password must be at least " + minPasswordLength + " characters");
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthResponse(
                accessToken,
                refreshToken,
                AppConstants.TOKEN_TYPE_BEARER,
                jwtService.getAccessExpirationMs() / 1000,
                toUserSummary(user)
        );
    }

    private UserSummaryDto toUserSummary(User user) {
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getIsActive()
        );
    }
}
