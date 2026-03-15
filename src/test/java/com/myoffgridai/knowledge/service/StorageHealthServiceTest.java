package com.myoffgridai.knowledge.service;

import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link StorageHealthService}.
 */
@ExtendWith(MockitoExtension.class)
class StorageHealthServiceTest {

    @Mock private SystemConfigService systemConfigService;

    private StorageHealthService storageHealthService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageHealthService = new StorageHealthService(systemConfigService);
    }

    @Test
    void checkStorageDirectory_existingDir_doesNotThrow() {
        SystemConfig config = new SystemConfig();
        config.setKnowledgeStoragePath(tempDir.toAbsolutePath().toString());
        when(systemConfigService.getConfig()).thenReturn(config);

        assertThatCode(() -> storageHealthService.checkStorageDirectory())
                .doesNotThrowAnyException();
    }

    @Test
    void checkStorageDirectory_nonExistentDir_createsIt() {
        SystemConfig config = new SystemConfig();
        config.setKnowledgeStoragePath(tempDir.resolve("new-dir").toAbsolutePath().toString());
        when(systemConfigService.getConfig()).thenReturn(config);

        assertThatCode(() -> storageHealthService.checkStorageDirectory())
                .doesNotThrowAnyException();
    }

    @Test
    void checkStorageDirectory_uncreatablePath_doesNotThrow() {
        SystemConfig config = new SystemConfig();
        config.setKnowledgeStoragePath("/nonexistent/root/path/that/cannot/be/created");
        when(systemConfigService.getConfig()).thenReturn(config);

        assertThatCode(() -> storageHealthService.checkStorageDirectory())
                .doesNotThrowAnyException();
    }
}
