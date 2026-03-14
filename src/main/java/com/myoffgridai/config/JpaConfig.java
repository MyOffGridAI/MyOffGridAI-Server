package com.myoffgridai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA auditing configuration.
 *
 * <p>Separated from the main application class to prevent
 * {@code @EnableJpaAuditing} from interfering with {@code @WebMvcTest}
 * slice tests that do not load the JPA context.</p>
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
