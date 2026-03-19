package com.myoffgridai.settings.repository;

import com.myoffgridai.settings.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UserSettings} entities.
 */
@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {

    /**
     * Finds settings for a specific user.
     *
     * @param userId the user's ID
     * @return an {@link Optional} containing the settings, or empty if not yet created
     */
    Optional<UserSettings> findByUserId(UUID userId);
}
