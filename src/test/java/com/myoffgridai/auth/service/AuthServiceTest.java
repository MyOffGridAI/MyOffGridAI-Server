package com.myoffgridai.auth.service;

import com.myoffgridai.auth.dto.*;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.common.exception.DuplicateResourceException;
import com.myoffgridai.common.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-signing",
                3600000L,
                86400000L
        );
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, jwtService, passwordEncoder, "dev");
    }

    // ── Register Tests ──────────────────────────────────────────────────────

    @Test
    void register_success_shouldReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest("newuser", "new@test.com", "New User", "pass", Role.ROLE_MEMBER);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().username()).isEqualTo("newuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_shouldThrow() {
        RegisterRequest request = new RegisterRequest("existing", null, "User", "pass", null);
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void register_duplicateEmail_shouldThrow() {
        RegisterRequest request = new RegisterRequest("newuser", "existing@test.com", "User", "pass", null);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void register_shortPassword_shouldThrow() {
        RegisterRequest request = new RegisterRequest("newuser", null, "User", "ab", null);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 4 characters");
    }

    @Test
    void register_nullRole_shouldDefaultToMember() {
        RegisterRequest request = new RegisterRequest("newuser", null, "User", "pass", null);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        AuthResponse response = authService.register(request);
        assertThat(response.user().role()).isEqualTo(Role.ROLE_MEMBER);
    }

    // ── Login Tests ─────────────────────────────────────────────────────────

    @Test
    void login_success_shouldReturnTokens() {
        User user = createTestUser();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        LoginRequest request = new LoginRequest("testuser", "pass");
        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.user().username()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class)); // updates lastLoginAt
    }

    @Test
    void login_wrongUsername_shouldThrow() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nonexistent", "pass")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_wrongPassword_shouldThrow() {
        User user = createTestUser();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_deactivatedUser_shouldThrow() {
        User user = createTestUser();
        user.setIsActive(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "pass")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("deactivated");
    }

    // ── Refresh Tests ───────────────────────────────────────────────────────

    @Test
    void refresh_validToken_shouldReturnNewAccessToken() {
        User user = createTestUser();
        String refreshToken = jwtService.generateRefreshToken(user);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        AuthResponse response = authService.refresh(refreshToken);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isEqualTo(refreshToken);
    }

    @Test
    void refresh_invalidToken_shouldThrow() {
        assertThatThrownBy(() -> authService.refresh("invalid.token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refresh_expiredToken_shouldThrow() {
        JwtService shortLived = new JwtService(
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-signing",
                -1000L, -1000L);
        AuthService shortAuthService = new AuthService(userRepository, shortLived, passwordEncoder, "dev");

        User user = createTestUser();
        String expired = shortLived.generateRefreshToken(user);

        assertThatThrownBy(() -> shortAuthService.refresh(expired))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_blacklistedToken_shouldThrow() {
        User user = createTestUser();
        String refreshToken = jwtService.generateRefreshToken(user);
        authService.logout(refreshToken);

        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("revoked");
    }

    // ── Logout Tests ────────────────────────────────────────────────────────

    @Test
    void logout_shouldBlacklistToken() {
        String token = "some-token";
        authService.logout(token);
        assertThat(authService.isTokenBlacklisted(token)).isTrue();
    }

    @Test
    void isTokenBlacklisted_notBlacklisted_shouldReturnFalse() {
        assertThat(authService.isTokenBlacklisted("fresh-token")).isFalse();
    }

    // ── ChangePassword Tests ────────────────────────────────────────────────

    @Test
    void changePassword_success() {
        User user = createTestUser();
        UUID userId = user.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        ChangePasswordRequest request = new ChangePasswordRequest("pass", "newpass");
        assertThatCode(() -> authService.changePassword(userId, request)).doesNotThrowAnyException();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_wrongCurrentPassword_shouldThrow() {
        User user = createTestUser();
        UUID userId = user.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ChangePasswordRequest request = new ChangePasswordRequest("wrong", "newpass");
        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void changePassword_userNotFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword(id, new ChangePasswordRequest("x", "y")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void changePassword_shortNewPassword_shouldThrow() {
        User user = createTestUser();
        UUID userId = user.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ChangePasswordRequest request = new ChangePasswordRequest("pass", "ab");
        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setDisplayName("Test User");
        user.setPasswordHash(passwordEncoder.encode("pass"));
        user.setRole(Role.ROLE_MEMBER);
        user.setIsActive(true);
        return user;
    }
}
