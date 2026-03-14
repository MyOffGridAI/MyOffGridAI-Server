package com.myoffgridai.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for JSON Web Token (JWT) generation, parsing, and validation.
 *
 * <p>Uses JJWT 0.12.x with HMAC-SHA256 signing. Token secrets and expiration
 * durations are sourced from application configuration — no hardcoded values.</p>
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    /**
     * Constructs the JWT service with configuration-driven secret and expiration values.
     *
     * @param secret              the HMAC signing secret (minimum 256 bits)
     * @param accessExpirationMs  access token lifetime in milliseconds
     * @param refreshExpirationMs refresh token lifetime in milliseconds
     */
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long accessExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Generates a short-lived access token for the given user.
     *
     * @param userDetails the authenticated user
     * @return a signed JWT access token
     */
    public String generateAccessToken(UserDetails userDetails) {
        log.debug("Generating access token for user: {}", userDetails.getUsername());
        return buildToken(new HashMap<>(), userDetails, accessExpirationMs);
    }

    /**
     * Generates a long-lived refresh token for the given user.
     *
     * @param userDetails the authenticated user
     * @return a signed JWT refresh token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        log.debug("Generating refresh token for user: {}", userDetails.getUsername());
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, userDetails, refreshExpirationMs);
    }

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token the JWT token string
     * @return the username embedded in the token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from a JWT token.
     *
     * @param token the JWT token string
     * @return the expiration {@link Date}
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Validates that a token belongs to the given user and has not expired.
     *
     * @param token       the JWT token string
     * @param userDetails the user to validate against
     * @return {@code true} if the token is valid for this user
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Checks whether a token has passed its expiration time.
     *
     * @param token the JWT token string
     * @return {@code true} if the token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Returns the configured access token expiration in milliseconds.
     *
     * @return access token expiration in ms
     */
    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expirationMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(signingKey)
                .compact();
    }
}
