package com.myoffgridai.system.repository;

import com.myoffgridai.system.model.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link SystemConfig} entities.
 *
 * <p>The {@code system_config} table is a singleton table — it must contain
 * exactly one row at all times after initial startup. The repository provides
 * both a safe single-row lookup ({@link #findFirst()}) and a full listing
 * ({@link #findAllOrderByUpdatedAtDesc()}) for deduplication recovery.</p>
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID> {

    /**
     * Retrieves the most recently updated system configuration row.
     *
     * <p>Uses a native query with {@code LIMIT 1} to guarantee at most one
     * result is returned, even if duplicate rows exist in the table.</p>
     *
     * @return the most recently updated system configuration, or empty if not yet created
     */
    @Query(value = "SELECT * FROM system_config ORDER BY updated_at DESC NULLS LAST LIMIT 1", nativeQuery = true)
    Optional<SystemConfig> findFirst();

    /**
     * Retrieves all system configuration rows ordered by most recently updated first.
     *
     * <p>Used for deduplication: when more than one row exists, the service
     * keeps the first (most recently updated) row and deletes the rest.</p>
     *
     * @return all system configuration rows, most recently updated first
     */
    @Query("SELECT s FROM SystemConfig s ORDER BY s.updatedAt DESC NULLS LAST")
    List<SystemConfig> findAllOrderByUpdatedAtDesc();
}
