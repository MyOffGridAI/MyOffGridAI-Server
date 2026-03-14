package com.myoffgridai.auth.service;

import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-signing",
                3600000L,   // 1 hour access
                86400000L   // 24 hours refresh
        );

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setPasswordHash("hashedpassword");
        testUser.setRole(Role.ROLE_MEMBER);
    }

    @Test
    void generateAccessToken_shouldReturnValidToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void generateRefreshToken_shouldReturnValidToken() {
        String token = jwtService.generateRefreshToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
    }

    @Test
    void extractExpiration_shouldReturnFutureDate() {
        String token = jwtService.generateAccessToken(testUser);
        Date expiration = jwtService.extractExpiration(token);

        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void isTokenValid_withMatchingUser_shouldReturnTrue() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    void isTokenValid_withDifferentUser_shouldReturnFalse() {
        String token = jwtService.generateAccessToken(testUser);

        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setRole(Role.ROLE_MEMBER);

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenExpired_withFreshToken_shouldReturnFalse() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void isTokenExpired_withExpiredToken_shouldReturnTrue() {
        JwtService shortLivedService = new JwtService(
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-signing",
                -1000L, // already expired
                -1000L
        );
        String token = shortLivedService.generateAccessToken(testUser);
        assertThat(shortLivedService.isTokenExpired(token)).isTrue();
    }

    @Test
    void extractUsername_withInvalidToken_shouldThrow() {
        assertThatThrownBy(() -> jwtService.extractUsername("invalid.token.here"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void getAccessExpirationMs_shouldReturnConfiguredValue() {
        assertThat(jwtService.getAccessExpirationMs()).isEqualTo(3600000L);
    }

    @Test
    void accessAndRefreshTokens_shouldBeDifferent() {
        String access = jwtService.generateAccessToken(testUser);
        String refresh = jwtService.generateRefreshToken(testUser);
        assertThat(access).isNotEqualTo(refresh);
    }
}
