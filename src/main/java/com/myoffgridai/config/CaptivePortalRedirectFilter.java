package com.myoffgridai.config;

import com.myoffgridai.system.service.ApModeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that redirects unrecognized requests to the captive portal
 * setup page when the device is in Access Point mode.
 *
 * <p>This filter triggers the captive portal popup on phones by redirecting
 * connectivity check requests (e.g., to Google, Apple domains) to the setup
 * wizard at {@code http://192.168.4.1:8080/setup}.</p>
 *
 * <p>Requests to {@code /api/}, {@code /setup}, {@code /actuator},
 * {@code /v3/api-docs}, and {@code /swagger-ui} are excluded.</p>
 */
@Component
@Order(1)
public class CaptivePortalRedirectFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CaptivePortalRedirectFilter.class);

    private final ApModeService apModeService;

    /**
     * Constructs the captive portal redirect filter.
     *
     * @param apModeService the AP mode service to check AP state
     */
    public CaptivePortalRedirectFilter(ApModeService apModeService) {
        this.apModeService = apModeService;
    }

    /**
     * Redirects requests to the setup wizard if AP mode is active and
     * the request path is not an API or setup path.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (apModeService.isApModeActive() && shouldRedirect(path)) {
            String redirectUrl = "http://" + AppConstants.AP_MODE_IP + ":"
                    + AppConstants.SERVER_PORT + "/setup";
            log.debug("Captive portal redirect: {} -> {}", path, redirectUrl);
            response.sendRedirect(redirectUrl);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determines whether the given path should be redirected to the setup wizard.
     *
     * @param path the request URI
     * @return true if the path should be redirected
     */
    private boolean shouldRedirect(String path) {
        return !path.startsWith("/api/")
                && !path.startsWith("/setup")
                && !path.startsWith("/actuator")
                && !path.startsWith("/v3/api-docs")
                && !path.startsWith("/swagger-ui");
    }
}
