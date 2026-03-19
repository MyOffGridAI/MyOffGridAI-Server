package com.myoffgridai.settings.service;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.settings.dto.UpdateUserSettingsRequest;
import com.myoffgridai.settings.dto.UserSettingsDto;
import com.myoffgridai.settings.model.UserSettings;
import com.myoffgridai.settings.repository.UserSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Manages per-user application settings with a get-or-create pattern.
 *
 * <p>On first access for a user, a default settings row is created automatically.
 * Subsequent accesses return and update the existing row.</p>
 */
@Service
public class UserSettingsService {

    private static final Logger log = LoggerFactory.getLogger(UserSettingsService.class);

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;

    /**
     * Constructs the service.
     *
     * @param userSettingsRepository the user settings repository
     * @param userRepository         the user repository
     */
    public UserSettingsService(UserSettingsRepository userSettingsRepository,
                               UserRepository userRepository) {
        this.userSettingsRepository = userSettingsRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns the current settings for the given user, creating defaults if none exist.
     *
     * @param userId the user's ID
     * @return the settings DTO
     */
    @Transactional
    public UserSettingsDto getSettings(UUID userId) {
        UserSettings settings = getOrCreateEntity(userId);
        return toDto(settings);
    }

    /**
     * Updates the settings for the given user and returns the updated DTO.
     *
     * @param userId  the user's ID
     * @param request the update request
     * @return the updated settings DTO
     */
    @Transactional
    public UserSettingsDto updateSettings(UUID userId, UpdateUserSettingsRequest request) {
        UserSettings settings = getOrCreateEntity(userId);
        settings.setThemePreference(request.themePreference());
        settings = userSettingsRepository.save(settings);
        log.info("Updated theme preference to '{}' for user {}", request.themePreference(), userId);
        return toDto(settings);
    }

    private UserSettings getOrCreateEntity(UUID userId) {
        return userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating default user settings for user {}", userId);
                    User user = userRepository.getReferenceById(userId);
                    UserSettings defaults = new UserSettings();
                    defaults.setUser(user);
                    return userSettingsRepository.save(defaults);
                });
    }

    private UserSettingsDto toDto(UserSettings entity) {
        return new UserSettingsDto(entity.getThemePreference());
    }
}
