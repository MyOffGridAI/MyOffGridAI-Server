package com.myoffgridai.privacy.aspect;

import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.privacy.model.AuditLog;
import com.myoffgridai.privacy.model.AuditOutcome;
import com.myoffgridai.privacy.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuditAspect}.
 */
@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditService auditService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private AuditAspect auditAspect;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        auditAspect = new AuditAspect(auditService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditAround_logsSuccessfulRequest() throws Throwable {
        // Set up request context
        request.setMethod("GET");
        request.setRequestURI("/api/v1/users");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "TestAgent/1.0");
        response.setStatus(200);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

        // Set up authenticated user
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setPasswordHash("hashed");
        user.setRole(Role.ROLE_MEMBER);
        user.setDisplayName("Test User");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Set up join point
        Object expectedResult = "success";
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // Use a concrete class as the target for extractResourceType
        TestController target = new TestController();
        when(joinPoint.getTarget()).thenReturn(target);

        // Execute
        Object result = auditAspect.auditControllerMethod(joinPoint);

        // Verify result
        assertEquals(expectedResult, result);

        // Verify audit log
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).logAction(captor.capture());
        AuditLog capturedLog = captor.getValue();

        assertEquals(userId, capturedLog.getUserId());
        assertEquals("testuser", capturedLog.getUsername());
        assertEquals("GET /api/v1/users", capturedLog.getAction());
        assertEquals("GET", capturedLog.getHttpMethod());
        assertEquals("/api/v1/users", capturedLog.getRequestPath());
        assertEquals("127.0.0.1", capturedLog.getIpAddress());
        assertEquals("TestAgent/1.0", capturedLog.getUserAgent());
        assertEquals(200, capturedLog.getResponseStatus());
        assertEquals(AuditOutcome.SUCCESS, capturedLog.getOutcome());
        assertEquals("Test", capturedLog.getResourceType());
        assertNotNull(capturedLog.getTimestamp());
    }

    @Test
    void auditAround_logsFailureOnException() throws Throwable {
        // Set up request context
        request.setMethod("POST");
        request.setRequestURI("/api/v1/data");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        SecurityContextHolder.clearContext();

        // Set up join point to throw
        RuntimeException expectedException = new RuntimeException("Something broke");
        when(joinPoint.proceed()).thenThrow(expectedException);

        TestController target = new TestController();
        when(joinPoint.getTarget()).thenReturn(target);

        // Execute and verify exception is rethrown
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> auditAspect.auditControllerMethod(joinPoint));
        assertEquals("Something broke", thrown.getMessage());

        // Verify audit log with FAILURE outcome
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).logAction(captor.capture());
        AuditLog capturedLog = captor.getValue();

        assertEquals(AuditOutcome.FAILURE, capturedLog.getOutcome());
        assertEquals(500, capturedLog.getResponseStatus());
    }

    @Test
    void auditAround_logsDeniedOutcome() throws Throwable {
        // Set up request context with 403 response
        request.setMethod("DELETE");
        request.setRequestURI("/api/v1/admin/config");
        response.setStatus(403);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        SecurityContextHolder.clearContext();

        // Set up join point
        when(joinPoint.proceed()).thenReturn(null);

        TestController target = new TestController();
        when(joinPoint.getTarget()).thenReturn(target);

        // Execute
        auditAspect.auditControllerMethod(joinPoint);

        // Verify audit log with DENIED outcome
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).logAction(captor.capture());
        AuditLog capturedLog = captor.getValue();

        assertEquals(AuditOutcome.DENIED, capturedLog.getOutcome());
        assertEquals(403, capturedLog.getResponseStatus());
    }

    @Test
    void auditAround_logsUnauthenticatedRequest() throws Throwable {
        // Set up request context with no authentication
        request.setMethod("GET");
        request.setRequestURI("/api/v1/public/health");
        response.setStatus(200);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        SecurityContextHolder.clearContext();

        // Set up join point
        when(joinPoint.proceed()).thenReturn("ok");

        TestController target = new TestController();
        when(joinPoint.getTarget()).thenReturn(target);

        // Execute
        Object result = auditAspect.auditControllerMethod(joinPoint);

        assertEquals("ok", result);

        // Verify audit log with null userId and username
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).logAction(captor.capture());
        AuditLog capturedLog = captor.getValue();

        assertNull(capturedLog.getUserId());
        assertNull(capturedLog.getUsername());
        assertEquals(AuditOutcome.SUCCESS, capturedLog.getOutcome());
    }

    @Test
    void auditAround_handlesAuditPersistenceFailure() throws Throwable {
        // Set up request context
        request.setMethod("GET");
        request.setRequestURI("/api/v1/test");
        response.setStatus(200);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        SecurityContextHolder.clearContext();

        // Set up join point
        Object expectedResult = "original result";
        when(joinPoint.proceed()).thenReturn(expectedResult);

        TestController target = new TestController();
        when(joinPoint.getTarget()).thenReturn(target);

        // Make auditService.logAction throw
        doThrow(new RuntimeException("DB connection lost")).when(auditService).logAction(any());

        // Execute - should NOT throw despite audit failure
        Object result = auditAspect.auditControllerMethod(joinPoint);

        // Original result should be returned
        assertEquals(expectedResult, result);

        // Verify logAction was called (and failed)
        verify(auditService).logAction(any(AuditLog.class));
    }

    /**
     * Stub controller class used as a target for the join point.
     * The aspect extracts the resource type from the class name by removing "Controller" suffix.
     */
    private static class TestController {
    }
}
