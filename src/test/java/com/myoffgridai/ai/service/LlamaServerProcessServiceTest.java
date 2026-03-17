package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.LlamaServerStatusDto;
import com.myoffgridai.config.LlamaServerProperties;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LlamaServerProcessService}.
 *
 * <p>Uses a {@link TempDir} for model files and mocked {@link ProcessBuilderFactory}
 * to avoid launching real processes.</p>
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class LlamaServerProcessServiceTest {

    @Mock private ProcessBuilderFactory processBuilderFactory;
    @Mock private ProcessBuilder mockProcessBuilder;
    @Mock private Process mockProcess;
    @Mock private SystemConfigService systemConfigService;

    @TempDir
    Path tempDir;

    private LlamaServerProperties properties;
    private LlamaServerProcessService service;
    private Path binaryPath;

    @BeforeEach
    void setUp() throws IOException {
        properties = new LlamaServerProperties();
        properties.setModelsDir(tempDir.toString());
        properties.setStartupTimeoutSeconds(2);
        properties.setHealthCheckIntervalSeconds(30);

        binaryPath = tempDir.resolve("llama-server");
        Files.writeString(binaryPath, "#!/bin/sh\necho 'fake'");
        properties.setBinary(binaryPath.toString());

        // Create model file
        Files.writeString(tempDir.resolve("test-model.gguf"), "fake gguf data");
        properties.setActiveModel("test-model.gguf");

        SystemConfig systemConfig = new SystemConfig();
        systemConfig.setActiveModelFilename("");
        when(systemConfigService.getConfig()).thenReturn(systemConfig);

        service = new LlamaServerProcessService(
                properties, systemConfigService, processBuilderFactory);
    }

    // ── start() returns early when no model configured ──────────────────

    @Test
    void start_noModelConfigured_staysStopped() {
        properties.setActiveModel("");
        SystemConfig config = new SystemConfig();
        config.setActiveModelFilename("");
        when(systemConfigService.getConfig()).thenReturn(config);

        service.start();

        verify(processBuilderFactory, never()).create(any());
        assertEquals("STOPPED", service.getStatus().status());
    }

    @Test
    void start_nullModelConfigured_staysStopped() {
        properties.setActiveModel(null);
        SystemConfig config = new SystemConfig();
        config.setActiveModelFilename(null);
        when(systemConfigService.getConfig()).thenReturn(config);

        service.start();

        verify(processBuilderFactory, never()).create(any());
        assertEquals("STOPPED", service.getStatus().status());
    }

    // ── start() returns ERROR when binary not found ─────────────────────

    @Test
    void start_binaryNotFound_setsError() {
        properties.setBinary("/nonexistent/llama-server");

        service.start();

        verify(processBuilderFactory, never()).create(any());
        LlamaServerStatusDto status = service.getStatus();
        assertEquals("ERROR", status.status());
        assertTrue(status.errorMessage().contains("binary not found"));
    }

    // ── start() returns ERROR when model file not found ─────────────────

    @Test
    void start_modelFileNotFound_setsError() {
        properties.setActiveModel("nonexistent-model.gguf");

        service.start();

        verify(processBuilderFactory, never()).create(any());
        LlamaServerStatusDto status = service.getStatus();
        assertEquals("ERROR", status.status());
        assertTrue(status.errorMessage().contains("Model file not found"));
    }

    // ── stop() terminates process gracefully ────────────────────────────

    @Test
    void stop_noProcess_doesNothing() {
        assertDoesNotThrow(() -> service.stop());
        assertEquals("STOPPED", service.getStatus().status());
    }

    @Test
    void stop_runningProcess_destroysIt() throws Exception {
        when(processBuilderFactory.create(any())).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.redirectErrorStream(true)).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.start()).thenReturn(mockProcess);
        when(mockProcess.pid()).thenReturn(200L);
        when(mockProcess.isAlive()).thenReturn(true);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.getInputStream())
                .thenReturn(new ByteArrayInputStream("log line\n".getBytes()));

        service.start();
        service.stop();

        verify(mockProcess).destroy();
        assertEquals("STOPPED", service.getStatus().status());
    }

    // ── switchModel() throws on blank filename ──────────────────────────

    @Test
    void switchModel_blankFilename_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.switchModel(""));
    }

    @Test
    void switchModel_nullFilename_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.switchModel(null));
    }

    @Test
    void switchModel_nonexistentFile_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.switchModel("does-not-exist.gguf"));
    }

    // ── getStatus() returns correct default state ───────────────────────

    @Test
    void getStatus_returnsStoppedByDefault() {
        LlamaServerStatusDto status = service.getStatus();

        assertEquals("STOPPED", status.status());
        assertNull(status.activeModelPath());
        assertEquals(1234, status.port());
        assertEquals(tempDir.toString(), status.modelsDir());
        assertNull(status.errorMessage());
        assertFalse(status.modelLoaded());
    }

    // ── getRecentLogLines() returns empty when no process ───────────────

    @Test
    void getRecentLogLines_noProcess_returnsEmpty() {
        assertTrue(service.getRecentLogLines(10).isEmpty());
    }

    // ── monitorHealth() behavior ────────────────────────────────────────

    @Test
    void monitorHealth_stoppedStatus_doesNothing() {
        // Should not throw or start anything
        assertDoesNotThrow(() -> service.monitorHealth());
    }

    // ── start() finds model in subdirectory ─────────────────────────────

    @Test
    void start_findsModelInSubdirectory() throws Exception {
        Path subDir = tempDir.resolve("org").resolve("model");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("deep-model.gguf"), "data");
        properties.setActiveModel("deep-model.gguf");

        when(processBuilderFactory.create(any())).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.redirectErrorStream(true)).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.start()).thenReturn(mockProcess);
        when(mockProcess.pid()).thenReturn(400L);
        when(mockProcess.isAlive()).thenReturn(true);
        when(mockProcess.getInputStream())
                .thenReturn(new ByteArrayInputStream("log\n".getBytes()));

        service.start();

        verify(processBuilderFactory).create(any());
    }

    // ── start() IOException does not propagate ──────────────────────────

    @Test
    void start_processIOException_setsErrorStatus() throws Exception {
        when(processBuilderFactory.create(any())).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.redirectErrorStream(true)).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.start()).thenThrow(new IOException("Cannot start"));

        assertDoesNotThrow(() -> service.start());
        assertEquals("ERROR", service.getStatus().status());
    }

    // ── destroy() delegates to stop() ───────────────────────────────────

    @Test
    void destroy_stopsProcess() {
        assertDoesNotThrow(() -> service.destroy());
        assertEquals("STOPPED", service.getStatus().status());
    }

    // ── start() uses SystemConfigService model first ────────────────────

    @Test
    void start_prefersSystemConfigModel() throws Exception {
        SystemConfig config = new SystemConfig();
        config.setActiveModelFilename("test-model.gguf");
        when(systemConfigService.getConfig()).thenReturn(config);

        when(processBuilderFactory.create(any())).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.redirectErrorStream(true)).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.start()).thenReturn(mockProcess);
        when(mockProcess.pid()).thenReturn(500L);
        when(mockProcess.isAlive()).thenReturn(true);
        when(mockProcess.getInputStream())
                .thenReturn(new ByteArrayInputStream("log\n".getBytes()));

        service.start();

        verify(processBuilderFactory).create(any());
    }
}
