package com.myoffgridai.system.service;

import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.dto.StorageSettingsDto;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SystemConfigService}.
 *
 * <p>Covers singleton getConfig() behavior including initialization on empty table,
 * normal single-row access, deduplication of multiple rows, and thread-safety
 * guarantees.</p>
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

    // ── getConfig(): 0 rows (initialization) ────────────────────────────

    @Test
    void getConfig_createsDefaultWhenEmpty() {
        SystemConfig defaultConfig = new SystemConfig();
        when(systemConfigRepository.findAllOrderByUpdatedAtDesc()).thenReturn(Collections.emptyList());
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(defaultConfig);

        SystemConfig result = systemConfigService.getConfig();

        assertNotNull(result);
        assertFalse(result.isInitialized());
        verify(systemConfigRepository).findAllOrderByUpdatedAtDesc();
        verify(systemConfigRepository).save(any(SystemConfig.class));
    }

    // ── getConfig(): exactly 1 row (normal) ─────────────────────────────

    @Test
    void getConfig_returnsExistingWhenOneRow() {
        SystemConfig existingConfig = new SystemConfig();
        existingConfig.setInitialized(true);
        existingConfig.setInstanceName("TestNode");

        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(existingConfig));

        SystemConfig result = systemConfigService.getConfig();

        assertEquals(existingConfig, result);
        assertTrue(result.isInitialized());
        assertEquals("TestNode", result.getInstanceName());
        verify(systemConfigRepository).findAllOrderByUpdatedAtDesc();
        verify(systemConfigRepository, never()).save(any());
        verify(systemConfigRepository, never()).deleteAll(anyList());
    }

    // ── getConfig(): 2+ rows (deduplication) ────────────────────────────

    @Test
    void getConfig_deduplicatesWhenTwoRowsExist() {
        SystemConfig newerConfig = new SystemConfig();
        newerConfig.setId(UUID.randomUUID());
        newerConfig.setInitialized(true);
        newerConfig.setInstanceName("Newer");
        newerConfig.setUpdatedAt(Instant.now());

        SystemConfig olderConfig = new SystemConfig();
        olderConfig.setId(UUID.randomUUID());
        olderConfig.setInitialized(false);
        olderConfig.setUpdatedAt(Instant.now().minusSeconds(60));

        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(new ArrayList<>(List.of(newerConfig, olderConfig)));

        SystemConfig result = systemConfigService.getConfig();

        assertEquals(newerConfig, result);
        assertTrue(result.isInitialized());
        assertEquals("Newer", result.getInstanceName());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SystemConfig>> captor = ArgumentCaptor.forClass(List.class);
        verify(systemConfigRepository).deleteAll(captor.capture());
        List<SystemConfig> deleted = captor.getValue();
        assertEquals(1, deleted.size());
        assertEquals(olderConfig.getId(), deleted.get(0).getId());
    }

    @Test
    void getConfig_deduplicatesWhenThreeRowsExist() {
        SystemConfig config1 = new SystemConfig();
        config1.setId(UUID.randomUUID());
        config1.setUpdatedAt(Instant.now());

        SystemConfig config2 = new SystemConfig();
        config2.setId(UUID.randomUUID());
        config2.setUpdatedAt(Instant.now().minusSeconds(30));

        SystemConfig config3 = new SystemConfig();
        config3.setId(UUID.randomUUID());
        config3.setUpdatedAt(Instant.now().minusSeconds(60));

        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(new ArrayList<>(List.of(config1, config2, config3)));

        SystemConfig result = systemConfigService.getConfig();

        assertEquals(config1, result);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SystemConfig>> captor = ArgumentCaptor.forClass(List.class);
        verify(systemConfigRepository).deleteAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    // ── getConfig(): thread-safety ──────────────────────────────────────

    @Test
    void getConfig_concurrentCallsCreateOnlyOneRow() throws InterruptedException {
        AtomicInteger saveCallCount = new AtomicInteger(0);

        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenAnswer(invocation -> {
                    if (saveCallCount.get() == 0) {
                        return Collections.emptyList();
                    }
                    return List.of(new SystemConfig());
                });
        when(systemConfigRepository.save(any(SystemConfig.class)))
                .thenAnswer(invocation -> {
                    saveCallCount.incrementAndGet();
                    return invocation.getArgument(0);
                });

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    systemConfigService.getConfig();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, saveCallCount.get(),
                "Only one save() call should occur even with concurrent getConfig() calls");
    }

    // ── save() ──────────────────────────────────────────────────────────

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

    // ── isInitialized() ─────────────────────────────────────────────────

    @Test
    void isInitialized_returnsTrue() {
        SystemConfig config = new SystemConfig();
        config.setInitialized(true);

        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));

        assertTrue(systemConfigService.isInitialized());
    }

    @Test
    void isInitialized_returnsFalse() {
        SystemConfig config = new SystemConfig();
        config.setInitialized(false);

        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));

        assertFalse(systemConfigService.isInitialized());
    }

    // ── setInitialized() ────────────────────────────────────────────────

    @Test
    void setInitialized_updatesConfigAndName() {
        SystemConfig config = new SystemConfig();
        config.setInitialized(false);
        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));

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

    // ── setFortressEnabled() ────────────────────────────────────────────

    @Test
    void setFortressEnabled_enablesFortress() {
        UUID userId = UUID.randomUUID();
        SystemConfig config = new SystemConfig();
        config.setFortressEnabled(false);
        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));

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
        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));

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

    // ── isWifiConfigured() ──────────────────────────────────────────────

    @Test
    void isWifiConfigured_returnsValue() {
        SystemConfig config = new SystemConfig();
        config.setWifiConfigured(true);

        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));

        assertTrue(systemConfigService.isWifiConfigured());
    }

    // ── AI Settings tests ───────────────────────────────────────────────

    @Test
    void getAiSettings_returnsDefaults() {
        SystemConfig config = new SystemConfig();
        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));

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
        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));

        SystemConfig savedConfig = new SystemConfig();
        savedConfig.setAiTemperature(1.0);
        savedConfig.setAiSimilarityThreshold(0.6);
        savedConfig.setAiMemoryTopK(10);
        savedConfig.setAiRagMaxContextTokens(4096);
        savedConfig.setAiContextSize(8192);
        savedConfig.setAiContextMessageLimit(50);
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(savedConfig);

        var dto = new AiSettingsDto(null, 1.0, 0.6, 10, 4096, 8192, 50);
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
        var dto = new AiSettingsDto(null, 2.5, null, null, null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateAiSettings(dto));
    }

    @Test
    void updateAiSettings_similarityThresholdOutOfRange_throws() {
        var dto = new AiSettingsDto(null, null, 1.5, null, null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateAiSettings(dto));
    }

    @Test
    void updateAiSettings_memoryTopKOutOfRange_throws() {
        var dto = new AiSettingsDto(null, null, null, 0, null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateAiSettings(dto));
    }

    @Test
    void updateAiSettings_ragMaxContextTokensOutOfRange_throws() {
        var dto = new AiSettingsDto(null, null, null, null, 100, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateAiSettings(dto));
    }

    @Test
    void updateAiSettings_contextSizeOutOfRange_throws() {
        var dto = new AiSettingsDto(null, null, null, null, null, 500, null);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateAiSettings(dto));
    }

    @Test
    void updateAiSettings_contextMessageLimitOutOfRange_throws() {
        var dto = new AiSettingsDto(null, null, null, null, null, null, 2);
        assertThrows(IllegalArgumentException.class,
                () -> systemConfigService.updateAiSettings(dto));
    }

    // ── Storage Settings tests ──────────────────────────────────────────

    @Test
    void getStorageSettings_includesMaxUploadSizeMb() {
        SystemConfig config = new SystemConfig();
        config.setMaxUploadSizeMb(50);
        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));

        StorageSettingsDto result = systemConfigService.getStorageSettings();

        assertEquals(50, result.maxUploadSizeMb());
    }

    @Test
    void getStorageSettings_defaultMaxUploadSizeMb() {
        SystemConfig config = new SystemConfig();
        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));

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
        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(config);

        var dto = new StorageSettingsDto("/var/myoffgridai/knowledge", null, null, null, 100);
        systemConfigService.updateStorageSettings(dto);

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).save(captor.capture());
        assertEquals(100, captor.getValue().getMaxUploadSizeMb());
    }

    // ── Active Model Filename ───────────────────────────────────────────

    @Test
    void getActiveModelFilename_returnsValue() {
        SystemConfig config = new SystemConfig();
        config.setActiveModelFilename("model.gguf");
        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));

        assertEquals("model.gguf", systemConfigService.getActiveModelFilename());
    }

    @Test
    void getActiveModelFilename_returnsNullWhenNotSet() {
        SystemConfig config = new SystemConfig();
        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));

        assertNull(systemConfigService.getActiveModelFilename());
    }

    @Test
    void setActiveModelFilename_savesFilename() {
        SystemConfig config = new SystemConfig();
        when(systemConfigRepository.findAllOrderByUpdatedAtDesc())
                .thenReturn(List.of(config));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(config);

        systemConfigService.setActiveModelFilename("new-model.gguf");

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).save(captor.capture());
        assertEquals("new-model.gguf", captor.getValue().getActiveModelFilename());
    }
}
