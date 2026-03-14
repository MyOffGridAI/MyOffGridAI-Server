package com.myoffgridai.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests using Testcontainers PostgreSQL.
 *
 * <p>Falls back to a direct JDBC connection if Testcontainers cannot
 * connect to Docker (e.g., Docker Desktop API mismatch).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class BaseIntegrationTest {

    private static final PostgreSQLContainer<?> postgres;
    private static final boolean useTestcontainers;

    static {
        PostgreSQLContainer<?> container = null;
        boolean tc = false;
        try {
            container = new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("myoffgridai_test")
                    .withUsername("test")
                    .withPassword("test");
            container.start();
            tc = true;
        } catch (Exception e) {
            // Testcontainers unavailable — fall back to Docker Compose Postgres
            container = null;
            tc = false;
        }
        postgres = container;
        useTestcontainers = tc;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (useTestcontainers && postgres != null) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        } else {
            registry.add("spring.datasource.url",
                    () -> "jdbc:postgresql://localhost:5432/myoffgridai");
            registry.add("spring.datasource.username", () -> "myoffgridai");
            registry.add("spring.datasource.password", () -> "myoffgridai");
        }
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
