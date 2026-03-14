package com.myoffgridai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * Configuration for pgvector integration with Spring Data JPA.
 *
 * <p>Verifies that the pgvector extension is installed in PostgreSQL
 * at application startup. Logs a warning if missing but does not
 * prevent startup — embedding features will fail gracefully at runtime.</p>
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructs the vector store configuration.
     *
     * @param jdbcTemplate the JDBC template for extension checks
     */
    public VectorStoreConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Checks for the pgvector extension on startup and logs the result.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkPgvectorExtension() {
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(
                    "SELECT extname FROM pg_extension WHERE extname = 'vector'");
            if (result.isEmpty()) {
                log.warn("pgvector extension not found in PostgreSQL. "
                        + "Embedding and RAG features will not work. "
                        + "Install with: CREATE EXTENSION vector;");
            } else {
                log.info("pgvector extension is available in PostgreSQL");
            }
        } catch (Exception e) {
            log.warn("Failed to check pgvector extension: {}", e.getMessage());
        }
    }
}
