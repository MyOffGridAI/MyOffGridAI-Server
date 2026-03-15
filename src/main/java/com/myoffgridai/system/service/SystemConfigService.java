package com.myoffgridai.system.service;

import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Manages the single-row system configuration for device-level settings.
 */
@Service
public class SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigService.class);

    private final SystemConfigRepository systemConfigRepository;

    /**
     * Constructs the system config service.
     *
     * @param systemConfigRepository the system config repository
     */
    public SystemConfigService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    /**
     * Fetches the single system config row, creating one with defaults if none exists.
     *
     * @return the system configuration
     */
    public SystemConfig getConfig() {
        return systemConfigRepository.findFirst()
                .orElseGet(() -> {
                    log.info("No system config found, creating default");
                    return systemConfigRepository.save(new SystemConfig());
                });
    }

    /**
     * Persists the system configuration.
     *
     * @param config the system config to save
     * @return the saved system config
     */
    public SystemConfig save(SystemConfig config) {
        return systemConfigRepository.save(config);
    }

    /**
     * Checks whether the system has been initialized (first-boot setup complete).
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return getConfig().isInitialized();
    }

    /**
     * Marks the system as initialized with the given instance name.
     *
     * @param instanceName the display name for this instance
     * @return the updated system config
     */
    public SystemConfig setInitialized(String instanceName) {
        SystemConfig config = getConfig();
        config.setInitialized(true);
        config.setInstanceName(instanceName);
        log.info("System initialized with instance name: {}", instanceName);
        return systemConfigRepository.save(config);
    }

    /**
     * Enables or disables fortress mode.
     *
     * @param enabled whether fortress mode should be enabled
     * @param userId  the ID of the user making the change
     * @return the updated system config
     */
    public SystemConfig setFortressEnabled(boolean enabled, UUID userId) {
        SystemConfig config = getConfig();
        config.setFortressEnabled(enabled);
        config.setFortressEnabledAt(enabled ? Instant.now() : null);
        config.setFortressEnabledByUserId(enabled ? userId : null);
        log.info("Fortress mode {}: by user {}", enabled ? "enabled" : "disabled", userId);
        return systemConfigRepository.save(config);
    }

    /**
     * Checks whether WiFi has been configured on this device.
     *
     * @return true if WiFi is configured
     */
    public boolean isWifiConfigured() {
        return getConfig().isWifiConfigured();
    }

    /**
     * Returns the current AI and memory settings from the system configuration.
     *
     * @return the AI settings DTO
     */
    public AiSettingsDto getAiSettings() {
        SystemConfig config = getConfig();
        return new AiSettingsDto(
                config.getAiModel(),
                config.getAiTemperature(),
                config.getAiSimilarityThreshold(),
                config.getAiMemoryTopK(),
                config.getAiRagMaxContextTokens()
        );
    }

    /**
     * Validates and updates AI and memory settings.
     *
     * @param dto the new AI settings
     * @return the updated AI settings DTO
     * @throws IllegalArgumentException if any value is out of range
     */
    public AiSettingsDto updateAiSettings(AiSettingsDto dto) {
        if (dto.temperature() != null && (dto.temperature() < 0.0 || dto.temperature() > 2.0)) {
            throw new IllegalArgumentException("Temperature must be between 0.0 and 2.0");
        }
        if (dto.similarityThreshold() != null && (dto.similarityThreshold() < 0.0 || dto.similarityThreshold() > 1.0)) {
            throw new IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0");
        }
        if (dto.memoryTopK() != null && (dto.memoryTopK() < 1 || dto.memoryTopK() > 20)) {
            throw new IllegalArgumentException("Memory Top-K must be between 1 and 20");
        }
        if (dto.ragMaxContextTokens() != null && (dto.ragMaxContextTokens() < 512 || dto.ragMaxContextTokens() > 8192)) {
            throw new IllegalArgumentException("RAG max context tokens must be between 512 and 8192");
        }

        SystemConfig config = getConfig();
        if (dto.modelName() != null && !dto.modelName().isBlank()) {
            config.setAiModel(dto.modelName());
        }
        if (dto.temperature() != null) {
            config.setAiTemperature(dto.temperature());
        }
        if (dto.similarityThreshold() != null) {
            config.setAiSimilarityThreshold(dto.similarityThreshold());
        }
        if (dto.memoryTopK() != null) {
            config.setAiMemoryTopK(dto.memoryTopK());
        }
        if (dto.ragMaxContextTokens() != null) {
            config.setAiRagMaxContextTokens(dto.ragMaxContextTokens());
        }

        SystemConfig saved = systemConfigRepository.save(config);
        log.info("AI settings updated: model={}, temperature={}, similarityThreshold={}, memoryTopK={}, ragMaxContextTokens={}",
                saved.getAiModel(), saved.getAiTemperature(), saved.getAiSimilarityThreshold(),
                saved.getAiMemoryTopK(), saved.getAiRagMaxContextTokens());

        return new AiSettingsDto(
                saved.getAiModel(),
                saved.getAiTemperature(),
                saved.getAiSimilarityThreshold(),
                saved.getAiMemoryTopK(),
                saved.getAiRagMaxContextTokens()
        );
    }
}
