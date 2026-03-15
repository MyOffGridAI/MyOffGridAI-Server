package com.myoffgridai.integration;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests using Testcontainers PostgreSQL.
 *
 * <p>Starts an isolated Testcontainers PostgreSQL instance so integration
 * tests never touch the dev database. If Docker is not available,
 * the entire test class is DISABLED (not errored, not fallen back to dev DB).</p>
 *
 * <p><strong>IMPORTANT:</strong> Integration tests must NEVER connect to
 * the dev database ({@code localhost:5432/myoffgridai}). All integration
 * tests run against a disposable Testcontainers instance.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(BaseIntegrationTest.DockerAvailableCondition.class)
abstract class BaseIntegrationTest {

    static PostgreSQLContainer<?> postgres;
    static boolean dockerAvailable = false;

    static {
        try {
            postgres = new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("myoffgridai_test")
                    .withUsername("test")
                    .withPassword("test");
            postgres.start();
            dockerAvailable = true;
        } catch (Exception e) {
            // Docker unavailable — tests will be disabled via DockerAvailableCondition
            dockerAvailable = false;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (dockerAvailable && postgres != null) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    /**
     * JUnit 5 {@link ExecutionCondition} that disables the entire test class
     * when Docker is not available. This runs BEFORE Spring context loading,
     * preventing context initialization failures when there is no database.
     */
    static class DockerAvailableCondition implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(
                ExtensionContext context) {
            if (dockerAvailable) {
                return ConditionEvaluationResult.enabled("Docker is available");
            }
            return ConditionEvaluationResult.disabled(
                    "Docker is not available — skipping integration tests");
        }
    }
}
