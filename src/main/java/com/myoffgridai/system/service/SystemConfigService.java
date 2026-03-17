package com.myoffgridai.system.service;

import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.dto.StorageSettingsDto;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                config.getAiRagMaxContextTokens(),
                config.getAiContextSize(),
                config.getAiContextMessageLimit()
        );
    }

    /**
     * Returns the current storage settings including disk usage for the knowledge storage path.
     *
     * @return the storage settings DTO
     */
    public StorageSettingsDto getStorageSettings() {
        SystemConfig config = getConfig();
        String storagePath = config.getKnowledgeStoragePath();
        File storageDir = new File(storagePath);

        long totalSpaceMb = 0;
        long usedSpaceMb = 0;
        long freeSpaceMb = 0;

        if (storageDir.exists()) {
            totalSpaceMb = storageDir.getTotalSpace() / (1024 * 1024);
            freeSpaceMb = storageDir.getUsableSpace() / (1024 * 1024);
            usedSpaceMb = totalSpaceMb - freeSpaceMb;
        }

        return new StorageSettingsDto(storagePath, totalSpaceMb, usedSpaceMb, freeSpaceMb, config.getMaxUploadSizeMb());
    }

    /**
     * Validates and updates the knowledge storage path.
     *
     * @param dto the new storage settings (only knowledgeStoragePath is used)
     * @return the updated storage settings DTO with current disk usage
     * @throws IllegalArgumentException if the path is not absolute or not writable
     */
    public StorageSettingsDto updateStorageSettings(StorageSettingsDto dto) {
        if (dto.knowledgeStoragePath() == null || dto.knowledgeStoragePath().isBlank()) {
            throw new IllegalArgumentException("Storage path must not be empty");
        }

        Path path = Paths.get(dto.knowledgeStoragePath());
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("Storage path must be an absolute path");
        }

        File dir = path.toFile();
        if (dir.exists() && !dir.isDirectory()) {
            throw new IllegalArgumentException("Storage path exists but is not a directory");
        }
        if (dir.exists() && !dir.canWrite()) {
            throw new IllegalArgumentException("Storage path is not writable");
        }

        if (dto.maxUploadSizeMb() != null && (dto.maxUploadSizeMb() < 1 || dto.maxUploadSizeMb() > 100)) {
            throw new IllegalArgumentException("Max upload size must be between 1 and 100 MB");
        }

        SystemConfig config = getConfig();
        config.setKnowledgeStoragePath(dto.knowledgeStoragePath());
        if (dto.maxUploadSizeMb() != null) {
            config.setMaxUploadSizeMb(dto.maxUploadSizeMb());
        }
        systemConfigRepository.save(config);
        log.info("Storage settings updated — path: {}, maxUploadSizeMb: {}",
                dto.knowledgeStoragePath(), config.getMaxUploadSizeMb());

        return getStorageSettings();
    }

    /**
     * Returns the active model filename from the system configuration.
     *
     * @return the active model filename, or null if not set
     */
    public String getActiveModelFilename() {
        return getConfig().getActiveModelFilename();
    }

    /**
     * Sets the active model filename in the system configuration.
     *
     * @param filename the GGUF model filename to persist
     */
    public void setActiveModelFilename(String filename) {
        SystemConfig config = getConfig();
        config.setActiveModelFilename(filename);
        systemConfigRepository.save(config);
        log.info("Active model filename set to: {}", filename);
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
        if (dto.contextSize() != null && (dto.contextSize() < 1024 || dto.contextSize() > 131072)) {
            throw new IllegalArgumentException("Context size must be between 1024 and 131072");
        }
        if (dto.contextMessageLimit() != null && (dto.contextMessageLimit() < 5 || dto.contextMessageLimit() > 100)) {
            throw new IllegalArgumentException("Context message limit must be between 5 and 100");
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
        if (dto.contextSize() != null) {
            config.setAiContextSize(dto.contextSize());
        }
        if (dto.contextMessageLimit() != null) {
            config.setAiContextMessageLimit(dto.contextMessageLimit());
        }

        SystemConfig saved = systemConfigRepository.save(config);
        log.info("AI settings updated: model={}, temperature={}, similarityThreshold={}, memoryTopK={}, ragMaxContextTokens={}, contextSize={}, contextMessageLimit={}",
                saved.getAiModel(), saved.getAiTemperature(), saved.getAiSimilarityThreshold(),
                saved.getAiMemoryTopK(), saved.getAiRagMaxContextTokens(),
                saved.getAiContextSize(), saved.getAiContextMessageLimit());

        return new AiSettingsDto(
                saved.getAiModel(),
                saved.getAiTemperature(),
                saved.getAiSimilarityThreshold(),
                saved.getAiMemoryTopK(),
                saved.getAiRagMaxContextTokens(),
                saved.getAiContextSize(),
                saved.getAiContextMessageLimit()
        );
    }
}
