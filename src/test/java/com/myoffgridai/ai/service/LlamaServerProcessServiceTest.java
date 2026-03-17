package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.LlamaServerStatusDto;
import com.myoffgridai.config.LlamaServerProperties;
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
 * <p>Uses a {@link TempDir} for model files, a mocked {@link ProcessBuilderFactory}
 * to avoid launching real processes, and mocked dependencies for
 * {@link SystemConfigService} and {@link InferenceService}.</p>
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class LlamaServerProcessServiceTest {

    @Mock private SystemConfigService systemConfigService;
    @Mock private ProcessBuilderFactory processBuilderFactory;
    @Mock private InferenceService inferenceService;
    @Mock private ProcessBuilder mockProcessBuilder;
    @Mock private Process mockProcess;

    @TempDir
    Path tempDir;

    private LlamaServerProperties properties;
    private LlamaServerProcessService service;
    private Path binaryPath;

    @BeforeEach
    void setUp() throws IOException {
        properties = new LlamaServerProperties();
        properties.setModelsDir(tempDir.toString());
        properties.setPort(1234);
        properties.setContextSize(4096);
        properties.setGpuLayers(0);
        properties.setThreads(2);
        properties.setHealthCheckIntervalSeconds(30);
        properties.setRestartDelaySeconds(0);
        properties.setStartupTimeoutSeconds(2);
        properties.setActiveModel("");

        // Create a fake binary
        binaryPath = tempDir.resolve("llama-server");
        Files.writeString(binaryPath, "#!/bin/sh\necho 'fake'");
        properties.setLlamaServerBinary(binaryPath.toString());

        service = new LlamaServerProcessService(
                properties, systemConfigService, processBuilderFactory, inferenceService);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  start tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void start_happyPath_statusRunning() throws Exception {
        Path modelFile = tempDir.resolve("test-model.gguf");
        Files.writeString(modelFile, "fake gguf data");
        properties.setActiveModel("test-model.gguf");

        when(processBuilderFactory.create(any())).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.redirectErrorStream(true)).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.start()).thenReturn(mockProcess);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("loaded model\n".getBytes()));
        when(mockProcess.pid()).thenReturn(12345L);
        when(mockProcess.isAlive()).thenReturn(true);
        when(inferenceService.isAvailable()).thenReturn(true);

        service.start();

        LlamaServerStatusDto status = service.getStatus();
        assertEquals(LlamaServerStatus.RUNNING, status.status());
        assertEquals("test-model.gguf", status.activeModel());
        assertEquals(1234, status.port());
        assertNull(status.errorMessage());
    }

    @Test
    void start_binaryNotFound_statusError() {
        properties.setLlamaServerBinary("/nonexistent/llama-server");
        properties.setActiveModel("model.gguf");

        // Create the model file so we pass model resolution
        try {
            Files.writeString(tempDir.resolve("model.gguf"), "data");
        } catch (IOException ignored) {}

        when(systemConfigService.getActiveModelFilename()).thenReturn(null);

        service.start();

        LlamaServerStatusDto status = service.getStatus();
        assertEquals(LlamaServerStatus.ERROR, status.status());
        assertTrue(status.errorMessage().contains("binary not found"));
    }

    @Test
    void start_noModelConfigured_statusStopped() {
        properties.setActiveModel("");
        when(systemConfigService.getActiveModelFilename()).thenReturn(null);

        service.start();

        LlamaServerStatusDto status = service.getStatus();
        assertEquals(LlamaServerStatus.STOPPED, status.status());
        assertNull(status.activeModel());
    }

    @Test
    void start_healthCheckTimeout_statusError() throws Exception {
        Path modelFile = tempDir.resolve("timeout-model.gguf");
        Files.writeString(modelFile, "fake");
        properties.setActiveModel("timeout-model.gguf");

        when(processBuilderFactory.create(any())).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.redirectErrorStream(true)).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.start()).thenReturn(mockProcess);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("starting...\n".getBytes()));
        when(mockProcess.pid()).thenReturn(99L);
        when(mockProcess.isAlive()).thenReturn(true);
        when(inferenceService.isAvailable()).thenReturn(false);

        service.start();

        LlamaServerStatusDto status = service.getStatus();
        assertEquals(LlamaServerStatus.ERROR, status.status());
        assertTrue(status.errorMessage().contains("timeout"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  stop tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void stop_runningProcess_statusStopped() throws Exception {
        // Start first
        Path modelFile = tempDir.resolve("stop-model.gguf");
        Files.writeString(modelFile, "fake");
        properties.setActiveModel("stop-model.gguf");

        when(processBuilderFactory.create(any())).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.redirectErrorStream(true)).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.start()).thenReturn(mockProcess);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("ok\n".getBytes()));
        when(mockProcess.pid()).thenReturn(42L);
        when(mockProcess.isAlive()).thenReturn(true);
        when(inferenceService.isAvailable()).thenReturn(true);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);

        service.start();
        assertEquals(LlamaServerStatus.RUNNING, service.getStatus().status());

        service.stop();
        assertEquals(LlamaServerStatus.STOPPED, service.getStatus().status());
        assertNull(service.getActiveModelPath());
    }

    @Test
    void stop_notRunning_statusStopped() {
        service.stop();
        assertEquals(LlamaServerStatus.STOPPED, service.getStatus().status());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  switchModel tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void switchModel_existingFile_restartsWithNewModel() throws Exception {
        Path modelFile = tempDir.resolve("new-model.gguf");
        Files.writeString(modelFile, "data");

        when(processBuilderFactory.create(any())).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.redirectErrorStream(true)).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.start()).thenReturn(mockProcess);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("ok\n".getBytes()));
        when(mockProcess.pid()).thenReturn(55L);
        when(mockProcess.isAlive()).thenReturn(true);
        when(inferenceService.isAvailable()).thenReturn(true);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(systemConfigService.getActiveModelFilename()).thenReturn("new-model.gguf");

        LlamaServerStatusDto result = service.switchModel("new-model.gguf");

        verify(systemConfigService).setActiveModelFilename("new-model.gguf");
        assertNotNull(result);
    }

    @Test
    void switchModel_fileNotFound_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.switchModel("nonexistent.gguf"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  monitorHealth tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void monitorHealth_stoppedStatus_doesNothing() {
        // Status is STOPPED by default
        service.monitorHealth();
        // No restart triggered
        verify(processBuilderFactory, never()).create(any());
    }

    @Test
    void monitorHealth_runningProcessAlive_doesNothing() throws Exception {
        // Start the process first
        Path modelFile = tempDir.resolve("health-model.gguf");
        Files.writeString(modelFile, "fake");
        properties.setActiveModel("health-model.gguf");

        when(processBuilderFactory.create(any())).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.redirectErrorStream(true)).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.start()).thenReturn(mockProcess);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("ok\n".getBytes()));
        when(mockProcess.pid()).thenReturn(77L);
        when(mockProcess.isAlive()).thenReturn(true);
        when(inferenceService.isAvailable()).thenReturn(true);

        service.start();
        assertEquals(LlamaServerStatus.RUNNING, service.getStatus().status());

        // Monitor should not restart since process is alive
        service.monitorHealth();

        // Only one call to create (from start), not two
        verify(processBuilderFactory, times(1)).create(any());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getRecentLogLines tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void getRecentLogLines_returnsEmptyWhenNoLogs() {
        assertTrue(service.getRecentLogLines(10).isEmpty());
    }

    @Test
    void getRecentLogLines_capsByRequestedCount() throws Exception {
        // Start with a process that emits multiple log lines
        StringBuilder logOutput = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            logOutput.append("log line ").append(i).append("\n");
        }

        Path modelFile = tempDir.resolve("log-model.gguf");
        Files.writeString(modelFile, "fake");
        properties.setActiveModel("log-model.gguf");

        when(processBuilderFactory.create(any())).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.redirectErrorStream(true)).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.start()).thenReturn(mockProcess);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream(logOutput.toString().getBytes()));
        when(mockProcess.pid()).thenReturn(88L);
        when(mockProcess.isAlive()).thenReturn(true);
        when(inferenceService.isAvailable()).thenReturn(true);

        service.start();

        // Give log thread time to consume
        Thread.sleep(200);

        var lines = service.getRecentLogLines(5);
        assertTrue(lines.size() <= 5);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getStatus tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void getStatus_initialState() {
        LlamaServerStatusDto status = service.getStatus();
        assertEquals(LlamaServerStatus.STOPPED, status.status());
        assertNull(status.activeModel());
        assertEquals(1234, status.port());
        assertNotNull(status.recentLogLines());
        assertTrue(status.recentLogLines().isEmpty());
    }
}
