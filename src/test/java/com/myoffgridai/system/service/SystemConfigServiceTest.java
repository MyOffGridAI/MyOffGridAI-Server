package com.myoffgridai.system.service;

import com.myoffgridai.system.dto.StorageSettingsDto;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SystemConfigService}.
 */
@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    private SystemConfigService systemConfigService;

    @BeforeEach
    void setUp() {
        systemConfigService = new SystemConfigService(systemConfigRepository);
    }

    @Test
    void getConfig_returnsExisting() {
        SystemConfig existingConfig = new SystemConfig();
        existingConfig.setInitialized(true);
        existingConfig.setInstanceName("TestNode");

        when(systemConfigRepository.findFirst()).thenReturn(Optional.of(existingConfig));

        SystemConfig result = systemConfigService.getConfig();

        assertEquals(existingConfig, result);
        assertTrue(result.isInitialized());
        assertEquals("TestNode", result.getInstanceName());
        verify(systemConfigRepository).findFirst();
        verify(systemConfigRepository, never()).save(any());
    }

    @Test
    void getConfig_createsDefaultWhenEmpty() {
        SystemConfig defaultConfig = new SystemConfig();
        when(systemConfigRepository.findFirst()).thenReturn(Optional.empty());
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(defaultConfig);

        SystemConfig result = systemConfigService.getConfig();

        assertNotNull(result);
        assertFalse(result.isInitialized());
        verify(systemConfigRepository).findFirst();
        verify(systemConfigRepository).save(any(SystemConfig.class));
    }

    @Test
    void save_delegatesToRepository() {
        SystemConfig config = new SystemConfig();
        config.setInstanceName("SavedNode");
        SystemConfig savedConfig = new SystemConfig();
        savedConfig.setInstanceName("SavedNode");

        when(systemConfigRepository.save(config)).thenReturn(savedConfig);

        SystemConfig result = systemConfigService.save(config);

        assertEquals("SavedNode", result.getInstanceName());
        verify(systemConfigRepository).save(config);
    }

    @Test
    void isInitialized_returnsTrue() {
        SystemConfig config = new SystemConfig();
        config.setInitialized(true);

        when(systemConfigRepository.findFirst()).thenReturn(Optional.of(config));

        assertTrue(systemConfigService.isInitialized());
    }

    @Test
    void isInitialized_returnsFalse() {
        SystemConfig config = new SystemConfig();
        config.setInitialized(false);

        when(systemConfigRepository.findFirst()).thenReturn(Optional.of(config));

        assertFalse(systemConfigService.isInitialized());
    }

    @Test
    void setInitialized_updatesConfigAndName() {
        SystemConfig config = new SystemConfig();
        config.setInitialized(false);
        when(systemConfigRepository.findFirst()).thenReturn(Optional.of(config));

        SystemConfig savedConfig = new SystemConfig();
        savedConfig.setInitialized(true);
        savedConfig.setInstanceName("MyNode");
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(savedConfig);

        SystemConfig result = systemConfigService.setInitialized("MyNode");

        assertTrue(result.isInitialized());
        assertEquals("MyNode", result.getInstanceName());

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).save(captor.capture());
        SystemConfig captured = captor.getValue();
        assertTrue(captured.isInitialized());
        assertEquals("MyNode", captured.getInstanceName());
    }

    @Test
    void setFortressEnabled_enablesFortress() {
        UUID userId = UUID.randomUUID();
        SystemConfig config = new SystemConfig();
        config.setFortressEnabled(false);
        when(systemConfigRepository.findFirst()).thenReturn(Optional.of(config));

        SystemConfig savedConfig = new SystemConfig();
        savedConfig.setFortressEnabled(true);
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(savedConfig);

        SystemConfig result = systemConfigService.setFortressEnabled(true, userId);

        assertTrue(result.isFortressEnabled());

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).save(captor.capture());
        SystemConfig captured = captor.getValue();
        assertTrue(captured.isFortressEnabled());
        assertNotNull(captured.getFortressEnabledAt());
        assertEquals(userId, captured.getFortressEnabledByUserId());
    }

    @Test
    void setFortressEnabled_disablesFortress() {
        UUID userId = UUID.randomUUID();
        SystemConfig config = new SystemConfig();
        config.setFortressEnabled(true);
        config.setFortressEnabledByUserId(userId);
        when(systemConfigRepository.findFirst()).thenReturn(Optional.of(config));

        SystemConfig savedConfig = new SystemConfig();
        savedConfig.setFortressEnabled(false);
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(savedConfig);

        SystemConfig result = systemConfigService.setFortressEnabled(false, userId);

        assertFalse(result.isFortressEnabled());

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).save(captor.capture());
        SystemConfig captured = captor.getValue();
        assertFalse(captured.isFortressEnabled());
        assertNull(captured.getFortressEnabledAt());
        assertNull(captured.getFortressEnabledByUserId());
    }

    @Test
    void isWifiConfigured_returnsValue() {
        SystemConfig config = new SystemConfig();
        config.setWifiConfigured(true);

        when(systemConfigRepository.findFirst()).thenReturn(Optional.of(config));

        assertTrue(systemConfigService.isWifiConfigured());
    }

    // ── AI Settings tests ────────────────────────────────────────────────

    @Test
    void getAiSettings_returnsDefaults() {
        SystemConfig config = new SystemConfig();
        when(systemConfigRepository.findFirst()).thenReturn(Optional.of(config));

        var result = systemConfigService.getAiSettings();

        assertEquals("hf.co/Qwen/Qwen3-32B-GGUF:Q4_K_M", result.modelName());
        assertEquals(0.7, result.temperature());
        assertEquals(0.45, result.similarityThreshold());
        assertEquals(5, result.memoryTopK());
        assertEquals(2048, result.ragMaxContextTokens());
        assertEquals(4096, result.contextSize());
        assertEquals(20, result.contextMessageLimit());
    }

    @Test
    void updateAiSettings_success() {
        SystemConfig config = new SystemConfig();
        when(systemConfigRepository.findFirst()).thenReturn(Optional.of(config));

        SystemConfig savedConfig = new SystemConfig();
        savedConfig.setAiTemperature(1.0);
        savedConfig.setAiSimilarityThreshold(0.6);
        savedConfig.setAiMemoryTopK(10);
        savedConfig.setAiRagMaxContextTokens(4096);
        savedConfig.setAiContextSize(8192);
        savedConfig.setAiContextMessageLimit(50);
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(savedConfig);

        var dto = new com.myoffgridai.system.dto.AiSettingsDto(null, 1.0, 0.6, 10, 4096, 8192, 50);
        var result = systemConfigService.updateAiSettings(dto);

        assertEquals(1.0, result.temperature());
        assertEquals(0.6, result.similarityThreshold());
        assertEquals(10, result.memoryTopK());
        assertEquals(4096, result.ragMaxContextTokens());
        assertEquals(8192, result.contextSize());
        assertEquals(50, result.contextMessageLimit());
    }

    @Test
    void updateAiSettings_temperatureOutOfRange_throws() {
        var dto = new com.myoffgridai.system.dto.AiSettingsDto(null, 2.5, null, null, null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateAiSettings(dto));
    }

    @Test
    void updateAiSettings_similarityThresholdOutOfRange_throws() {
        var dto = new com.myoffgridai.system.dto.AiSettingsDto(null, null, 1.5, null, null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateAiSettings(dto));
    }

    @Test
    void updateAiSettings_memoryTopKOutOfRange_throws() {
        var dto = new com.myoffgridai.system.dto.AiSettingsDto(null, null, null, 0, null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateAiSettings(dto));
    }

    @Test
    void updateAiSettings_ragMaxContextTokensOutOfRange_throws() {
        var dto = new com.myoffgridai.system.dto.AiSettingsDto(null, null, null, null, 100, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateAiSettings(dto));
    }

    @Test
    void updateAiSettings_contextSizeOutOfRange_throws() {
        var dto = new com.myoffgridai.system.dto.AiSettingsDto(null, null, null, null, null, 500, null);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateAiSettings(dto));
    }

    @Test
    void updateAiSettings_contextMessageLimitOutOfRange_throws() {
        var dto = new com.myoffgridai.system.dto.AiSettingsDto(null, null, null, null, null, null, 2);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateAiSettings(dto));
    }

    // ── Storage Settings tests ──────────────────────────────────────────

    @Test
    void getStorageSettings_includesMaxUploadSizeMb() {
        SystemConfig config = new SystemConfig();
        config.setMaxUploadSizeMb(50);
        when(systemConfigRepository.findFirst()).thenReturn(Optional.of(config));

        StorageSettingsDto result = systemConfigService.getStorageSettings();

        assertEquals(50, result.maxUploadSizeMb());
    }

    @Test
    void getStorageSettings_defaultMaxUploadSizeMb() {
        SystemConfig config = new SystemConfig();
        when(systemConfigRepository.findFirst()).thenReturn(Optional.of(config));

        StorageSettingsDto result = systemConfigService.getStorageSettings();

        assertEquals(25, result.maxUploadSizeMb());
    }

    @Test
    void updateStorageSettings_maxUploadSizeMb_tooLow_throws() {
        var dto = new StorageSettingsDto("/var/myoffgridai/knowledge", null, null, null, 0);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateStorageSettings(dto));
    }

    @Test
    void updateStorageSettings_maxUploadSizeMb_tooHigh_throws() {
        var dto = new StorageSettingsDto("/var/myoffgridai/knowledge", null, null, null, 101);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateStorageSettings(dto));
    }

    @Test
    void updateStorageSettings_maxUploadSizeMb_validBoundary() {
        SystemConfig config = new SystemConfig();
        when(systemConfigRepository.findFirst()).thenReturn(Optional.of(config));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(config);

        var dto = new StorageSettingsDto("/var/myoffgridai/knowledge", null, null, null, 100);
        systemConfigService.updateStorageSettings(dto);

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).save(captor.capture());
        assertEquals(100, captor.getValue().getMaxUploadSizeMb());
    }
}
