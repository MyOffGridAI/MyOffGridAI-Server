package com.myoffgridai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link MdcFilter}.
 */
@ExtendWith(MockitoExtension.class)
class MdcFilterTest {

    private MdcFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new MdcFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void doFilter_setsRequestId() throws ServletException, IOException {
        AtomicReference<String> capturedRequestId = new AtomicReference<>();

        filter.doFilterInternal(request, response, (req, res) -> {
            capturedRequestId.set(MDC.get(MdcFilter.MDC_REQUEST_ID));
        });

        assertNotNull(capturedRequestId.get());
        assertFalse(capturedRequestId.get().isEmpty());
    }

    @Test
    void doFilter_clearsMdcAfterRequest() throws ServletException, IOException {
        MDC.put("stale", "value");

        filter.doFilterInternal(request, response, (req, res) -> {
            // MDC should have requestId during request
            assertNotNull(MDC.get(MdcFilter.MDC_REQUEST_ID));
        });

        // MDC should be cleared after request
        assertNull(MDC.get(MdcFilter.MDC_REQUEST_ID));
        assertNull(MDC.get("stale"));
    }

    @Test
    void doFilter_withAuthenticatedUser_setsUsernameAndUserId() throws ServletException, IOException {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("testuser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        AtomicReference<String> capturedUsername = new AtomicReference<>();
        AtomicReference<String> capturedUserId = new AtomicReference<>();

        filter.doFilterInternal(request, response, (req, res) -> {
            capturedUsername.set(MDC.get(MdcFilter.MDC_USERNAME));
            capturedUserId.set(MDC.get(MdcFilter.MDC_USER_ID));
        });

        assertEquals("testuser", capturedUsername.get());
        assertEquals("testuser", capturedUserId.get());
    }

    @Test
    void doFilter_withAnonymousUser_doesNotSetUsername() throws ServletException, IOException {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        AtomicReference<String> capturedUsername = new AtomicReference<>();

        filter.doFilterInternal(request, response, (req, res) -> {
            capturedUsername.set(MDC.get(MdcFilter.MDC_USERNAME));
        });

        assertNull(capturedUsername.get());
    }

    @Test
    void doFilter_withNoAuthentication_doesNotSetUsername() throws ServletException, IOException {
        AtomicReference<String> capturedUsername = new AtomicReference<>();

        filter.doFilterInternal(request, response, (req, res) -> {
            capturedUsername.set(MDC.get(MdcFilter.MDC_USERNAME));
        });

        assertNull(capturedUsername.get());
    }

    @Test
    void doFilter_clearsMdcEvenOnException() throws ServletException, IOException {
        try {
            filter.doFilterInternal(request, response, (req, res) -> {
                assertNotNull(MDC.get(MdcFilter.MDC_REQUEST_ID));
                throw new ServletException("test exception");
            });
            fail("Expected ServletException");
        } catch (ServletException e) {
            assertEquals("test exception", e.getMessage());
        }

        // MDC must still be cleared
        assertNull(MDC.get(MdcFilter.MDC_REQUEST_ID));
    }

    @Test
    void doFilter_callsFilterChain() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }
}
