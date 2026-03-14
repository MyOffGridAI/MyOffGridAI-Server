package com.myoffgridai.privacy.aspect;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.privacy.model.AuditLog;
import com.myoffgridai.privacy.model.AuditOutcome;
import com.myoffgridai.privacy.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

/**
 * AOP aspect that automatically audits all controller endpoint invocations.
 * Captures HTTP method, path, user, response status, duration, and outcome.
 * Never logs request body content, passwords, tokens, or sensitive headers.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;

    /**
     * Constructs the audit aspect.
     *
     * @param auditService the audit service for log persistence
     */
    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Intercepts all public methods in controller classes across all packages,
     * captures request metadata, and logs an audit entry after execution.
     *
     * @param joinPoint the proceeding join point
     * @return the result of the controller method
     * @throws Throwable if the controller method throws
     */
    @Around("execution(public * com.myoffgridai.*.controller..*(..))")
    public Object auditControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attrs.getRequest();
        HttpServletResponse response = attrs.getResponse();

        UUID userId = null;
        String username = null;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User user) {
                userId = user.getId();
                username = user.getUsername();
            }
        } catch (Exception e) {
            // Unauthenticated — userId stays null
        }

        Object result;
        int responseStatus = 200;
        AuditOutcome outcome = AuditOutcome.SUCCESS;

        try {
            result = joinPoint.proceed();

            if (response != null) {
                responseStatus = response.getStatus();
            }

            if (responseStatus >= 400) {
                outcome = (responseStatus == 401 || responseStatus == 403)
                        ? AuditOutcome.DENIED
                        : AuditOutcome.FAILURE;
            }
        } catch (Exception e) {
            responseStatus = 500;
            outcome = AuditOutcome.FAILURE;
            throw e;
        } finally {
            try {
                long durationMs = System.currentTimeMillis() - startTime;
                String resourceType = extractResourceType(joinPoint.getTarget().getClass());

                AuditLog auditLog = new AuditLog();
                auditLog.setUserId(userId);
                auditLog.setUsername(username);
                auditLog.setAction(request.getMethod() + " " + request.getRequestURI());
                auditLog.setResourceType(resourceType);
                auditLog.setHttpMethod(request.getMethod());
                auditLog.setRequestPath(request.getRequestURI());
                auditLog.setIpAddress(request.getRemoteAddr());
                auditLog.setUserAgent(truncate(request.getHeader("User-Agent"), 500));
                auditLog.setResponseStatus(responseStatus);
                auditLog.setOutcome(outcome);
                auditLog.setDurationMs(durationMs);
                auditLog.setTimestamp(Instant.now());

                auditService.logAction(auditLog);
            } catch (Exception auditError) {
                log.warn("Failed to persist audit log: {}", auditError.getMessage());
            }
        }

        return result;
    }

    private String extractResourceType(Class<?> controllerClass) {
        String name = controllerClass.getSimpleName();
        if (name.endsWith("Controller")) {
            return name.substring(0, name.length() - "Controller".length());
        }
        return name;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
