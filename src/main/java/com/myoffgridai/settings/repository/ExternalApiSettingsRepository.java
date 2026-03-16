package com.myoffgridai.settings.repository;

import com.myoffgridai.settings.model.ExternalApiSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for the singleton {@link ExternalApiSettings} row.
 */
public interface ExternalApiSettingsRepository extends JpaRepository<ExternalApiSettings, UUID> {

    /**
     * Finds the singleton settings row by the guard column.
     *
     * @param singletonGuard the guard value (always "SINGLETON")
     * @return the settings if they exist
     */
    Optional<ExternalApiSettings> findBySingletonGuard(String singletonGuard);
}
