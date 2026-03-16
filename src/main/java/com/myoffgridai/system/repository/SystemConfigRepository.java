package com.myoffgridai.system.repository;

import com.myoffgridai.system.model.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link SystemConfig} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID> {

    /**
     * Retrieves the single system configuration row.
     *
     * @return the system configuration, or empty if not yet created
     */
    @Query("SELECT s FROM SystemConfig s")
    Optional<SystemConfig> findFirst();
}
