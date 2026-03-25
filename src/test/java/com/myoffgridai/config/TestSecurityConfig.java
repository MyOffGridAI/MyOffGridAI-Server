package com.myoffgridai.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that disables servlet filters
 * for {@code @WebMvcTest} controller tests.
 *
 * <p>Disables {@link JwtAuthFilter}, {@link CaptivePortalRedirectFilter},
 * {@link MdcFilter}, {@link RateLimitingFilter}, and
 * {@link RequestResponseLoggingFilter} via {@link FilterRegistrationBean}
 * beans with {@code setEnabled(false)}.</p>
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
public class TestSecurityConfig {

    /**
     * Configures a stateless security filter chain for tests with
     * the same public endpoint rules as production.
     *
     * @param http the HTTP security builder
     * @return the configured test security filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/system/status",
                                "/api/system/initialize",
                                "/api/system/finalize-setup",
                                "/api/setup/**",
                                "/api/models",
                                "/api/models/health",
                                "/setup/**",
                                "/mcp/**",
                                "/api/library/ebooks/*/cover"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"success\":false,\"message\":\"Unauthorized\"}");
                        })
                );

        return http.build();
    }

    /**
     * Disables the JWT authentication filter in test context.
     *
     * @param filter the JWT filter bean
     * @return a disabled filter registration
     */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> disableJwtFilter(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    /**
     * Disables the captive portal redirect filter in test context.
     *
     * @param filter the captive portal filter bean
     * @return a disabled filter registration
     */
    @Bean
    public FilterRegistrationBean<CaptivePortalRedirectFilter> disableCaptivePortalFilter(
            CaptivePortalRedirectFilter filter) {
        FilterRegistrationBean<CaptivePortalRedirectFilter> reg =
                new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    /**
     * Disables the MDC filter in test context.
     *
     * @param filter the MDC filter bean
     * @return a disabled filter registration
     */
    @Bean
    public FilterRegistrationBean<MdcFilter> disableMdcFilter(MdcFilter filter) {
        FilterRegistrationBean<MdcFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    /**
     * Disables the rate limiting filter in test context.
     *
     * @param filter the rate limiting filter bean
     * @return a disabled filter registration
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> disableRateLimitFilter(
            RateLimitingFilter filter) {
        FilterRegistrationBean<RateLimitingFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    /**
     * Disables the request/response logging filter in test context.
     *
     * @param filter the logging filter bean
     * @return a disabled filter registration
     */
    @Bean
    public FilterRegistrationBean<RequestResponseLoggingFilter> disableRequestResponseLoggingFilter(
            RequestResponseLoggingFilter filter) {
        FilterRegistrationBean<RequestResponseLoggingFilter> reg =
                new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}
