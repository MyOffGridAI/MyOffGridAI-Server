package com.myoffgridai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RateLimitingFilter}.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(objectMapper, true);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setRemoteAddr("127.0.0.1");
    }

    @Test
    void doFilter_underLimit_proceedsNormally() throws ServletException, IOException {
        request.setRequestURI("/api/ai/chat");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilter_authEndpoint_underLimit_proceeds() throws ServletException, IOException {
        request.setRequestURI("/api/auth/login");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_authEndpoint_exceedsLimit_returns429() throws ServletException, IOException {
        request.setRequestURI("/api/auth/login");

        // Exhaust the auth bucket (10 requests)
        for (int i = 0; i < AppConstants.RATE_LIMIT_AUTH_CAPACITY; i++) {
            MockHttpServletResponse r = new MockHttpServletResponse();
            filter.doFilterInternal(request, r, filterChain);
        }

        // Next request should be rejected
        MockHttpServletResponse rejectedResponse = new MockHttpServletResponse();
        filter.doFilterInternal(request, rejectedResponse, filterChain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), rejectedResponse.getStatus());
        assertTrue(rejectedResponse.getContentAsString().contains(AppConstants.RATE_LIMIT_EXCEEDED_MESSAGE));
    }

    @Test
    void doFilter_apiEndpoint_exceedsLimit_returns429() throws ServletException, IOException {
        request.setRequestURI("/api/ai/chat");

        // Exhaust the API bucket (200 requests)
        for (int i = 0; i < AppConstants.RATE_LIMIT_API_CAPACITY; i++) {
            MockHttpServletResponse r = new MockHttpServletResponse();
            filter.doFilterInternal(request, r, filterChain);
        }

        // Next request should be rejected
        MockHttpServletResponse rejectedResponse = new MockHttpServletResponse();
        filter.doFilterInternal(request, rejectedResponse, filterChain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), rejectedResponse.getStatus());
    }

    @Test
    void doFilter_disabled_alwaysProceeds() throws ServletException, IOException {
        filter = new RateLimitingFilter(objectMapper, false);
        request.setRequestURI("/api/auth/login");

        // Even with many requests, should always proceed when disabled
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse r = new MockHttpServletResponse();
            filter.doFilterInternal(request, r, filterChain);
        }

        verify(filterChain, times(20)).doFilter(eq(request), any());
    }

    @Test
    void doFilter_differentIps_haveSeparateBuckets() throws ServletException, IOException {
        request.setRequestURI("/api/auth/login");

        // Exhaust bucket for IP 1
        for (int i = 0; i < AppConstants.RATE_LIMIT_AUTH_CAPACITY; i++) {
            MockHttpServletResponse r = new MockHttpServletResponse();
            filter.doFilterInternal(request, r, filterChain);
        }

        // IP 2 should still work
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setRemoteAddr("192.168.1.100");
        request2.setRequestURI("/api/auth/login");
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        filter.doFilterInternal(request2, response2, filterChain);

        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response2.getStatus());
    }

    @Test
    void getClientIp_noXff_returnsRemoteAddr() {
        request.setRemoteAddr("10.0.0.1");
        assertEquals("10.0.0.1", filter.getClientIp(request));
    }

    @Test
    void getClientIp_withXff_returnsFirstIp() {
        request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18, 150.172.238.178");
        assertEquals("203.0.113.50", filter.getClientIp(request));
    }

    @Test
    void getClientIp_withEmptyXff_returnsRemoteAddr() {
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "");
        assertEquals("10.0.0.1", filter.getClientIp(request));
    }

    @Test
    void resolveBucket_authPath_returnsAuthBucket() {
        var bucket1 = filter.resolveBucket("1.2.3.4", "/api/auth/login");
        var bucket2 = filter.resolveBucket("1.2.3.4", "/api/auth/register");
        // Same IP, same auth tier — same bucket
        assertSame(bucket1, bucket2);
    }

    @Test
    void resolveBucket_apiPath_returnsApiBucket() {
        var bucket1 = filter.resolveBucket("1.2.3.4", "/api/ai/chat");
        var bucket2 = filter.resolveBucket("1.2.3.4", "/api/memory/search");
        // Same IP, same API tier — same bucket
        assertSame(bucket1, bucket2);
    }

    @Test
    void resolveBucket_authAndApi_differentBuckets() {
        var authBucket = filter.resolveBucket("1.2.3.4", "/api/auth/login");
        var apiBucket = filter.resolveBucket("1.2.3.4", "/api/ai/chat");
        assertNotSame(authBucket, apiBucket);
    }

    @Test
    void isEnabled_returnsConfiguredValue() {
        assertTrue(filter.isEnabled());

        RateLimitingFilter disabled = new RateLimitingFilter(objectMapper, false);
        assertFalse(disabled.isEnabled());
    }
}
