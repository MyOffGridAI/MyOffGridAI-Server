package com.myoffgridai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link RequestResponseLoggingFilter}.
 */
@ExtendWith(MockitoExtension.class)
class RequestResponseLoggingFilterTest {

    private RequestResponseLoggingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RequestResponseLoggingFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void doFilter_normalPath_callsFilterChain() throws ServletException, IOException {
        request.setRequestURI("/api/ai/chat");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_actuatorPath_callsFilterChain() throws ServletException, IOException {
        request.setRequestURI("/actuator/health");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_setupPath_callsFilterChain() throws ServletException, IOException {
        request.setRequestURI("/setup/wifi");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkip_actuator_returnsTrue() {
        assertTrue(filter.shouldSkip("/actuator/health"));
        assertTrue(filter.shouldSkip("/actuator/info"));
    }

    @Test
    void shouldSkip_setup_returnsTrue() {
        assertTrue(filter.shouldSkip("/setup"));
        assertTrue(filter.shouldSkip("/setup/wifi"));
    }

    @Test
    void shouldSkip_apiPath_returnsFalse() {
        assertFalse(filter.shouldSkip("/api/ai/chat"));
        assertFalse(filter.shouldSkip("/api/auth/login"));
    }

    @Test
    void shouldSkip_rootPath_returnsFalse() {
        assertFalse(filter.shouldSkip("/"));
    }

    @Test
    void doFilter_propagatesExceptions() {
        request.setRequestURI("/api/test");

        assertThrows(ServletException.class, () ->
                filter.doFilterInternal(request, response, (req, res) -> {
                    throw new ServletException("test error");
                }));
    }

    @Test
    void doFilter_setsResponseStatus() throws ServletException, IOException {
        request.setRequestURI("/api/test");
        request.setMethod("GET");

        filter.doFilterInternal(request, response, (req, res) -> {
            ((jakarta.servlet.http.HttpServletResponse) res).setStatus(201);
        });

        assertEquals(201, response.getStatus());
    }
}
