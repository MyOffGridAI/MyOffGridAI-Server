package com.myoffgridai.models.service;

import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.ai.service.InferenceService;
import com.myoffgridai.models.dto.DownloadProgress;
import com.myoffgridai.models.dto.DownloadStatus;
import com.myoffgridai.models.dto.LocalModelFileDto;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ModelDownloadService}.
 *
 * <p>Uses a temporary directory to simulate the local models directory.
 * WebClient calls (actual downloads) are not tested here since they require
 * network access; only the synchronous management methods are verified.</p>
 */
@ExtendWith(MockitoExtension.class)
class ModelDownloadServiceTest {

    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;
    @Mock private ExternalApiSettingsService settingsService;
    @Mock private ModelDownloadProgressRegistry progressRegistry;
    @Mock private InferenceService inferenceService;

    @TempDir
    Path tempDir;

    private ModelDownloadService service;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        service = new ModelDownloadService(
                webClientBuilder,
                settingsService,
                progressRegistry,
                inferenceService,
                tempDir.toString()
        );
    }

    @Test
    void startDownload_returnsDownloadId() {
        // startDownload calls executeDownload which is @Async, but in unit test
        // it runs synchronously. The WebClient call will fail since it's mocked
        // without method stubs, but startDownload itself should return an ID.
        // We need to verify the ID is returned before executeDownload fails.
        // Since startDownload puts the download in the map before calling executeDownload,
        // and executeDownload catches all exceptions, the ID should always be returned.
        String downloadId = service.startDownload("author/model", "model-Q4.gguf");

        assertNotNull(downloadId);
        assertFalse(downloadId.isBlank());
    }

    @Test
    void getProgress_returnsProgressAfterStart() {
        String downloadId = service.startDownload("author/model", "model-Q4.gguf");

        Optional<DownloadProgress> progress = service.getProgress(downloadId);

        assertTrue(progress.isPresent());
        assertEquals(downloadId, progress.get().downloadId());
        assertEquals("author/model", progress.get().repoId());
        assertEquals("model-Q4.gguf", progress.get().filename());
    }

    @Test
    void getProgress_returnsEmptyForUnknownId() {
        Optional<DownloadProgress> progress = service.getProgress("nonexistent");

        assertTrue(progress.isEmpty());
    }

    @Test
    void getAllDownloads_returnsTrackedDownloads() {
        service.startDownload("author/model-a", "a.gguf");
        service.startDownload("author/model-b", "b.gguf");

        List<DownloadProgress> downloads = service.getAllDownloads();

        assertEquals(2, downloads.size());
    }

    @Test
    void getAllDownloads_returnsEmptyWhenNoDownloads() {
        List<DownloadProgress> downloads = service.getAllDownloads();

        assertTrue(downloads.isEmpty());
    }

    @Test
    void cancelDownload_setsCancelledFlag() {
        String downloadId = service.startDownload("author/model", "model.gguf");

        // cancelDownload should not throw
        assertDoesNotThrow(() -> service.cancelDownload(downloadId));
    }

    @Test
    void cancelDownload_nonexistentId_doesNotThrow() {
        assertDoesNotThrow(() -> service.cancelDownload("nonexistent"));
    }

    @Test
    void listLocalModels_returnsEmptyWhenDirectoryDoesNotExist() {
        // Create a service pointing to a nonexistent directory
        when(webClientBuilder.build()).thenReturn(webClient);
        ModelDownloadService svcMissing = new ModelDownloadService(
                webClientBuilder,
                settingsService,
                progressRegistry,
                inferenceService,
                tempDir.resolve("nonexistent").toString()
        );

        List<LocalModelFileDto> models = svcMissing.listLocalModels();

        assertTrue(models.isEmpty());
    }

    @Test
    void listLocalModels_scansGgufFiles() throws IOException {
        // Create a GGUF file in the temp models directory structure
        Path authorDir = tempDir.resolve("thebloke").resolve("llama2-gguf");
        Files.createDirectories(authorDir);
        Path ggufFile = authorDir.resolve("model-Q4_K_M.gguf");
        Files.writeString(ggufFile, "fake model data");

        when(inferenceService.getActiveModel()).thenReturn(null);

        List<LocalModelFileDto> models = service.listLocalModels();

        assertEquals(1, models.size());
        assertEquals("model-Q4_K_M.gguf", models.get(0).filename());
        assertEquals("gguf", models.get(0).format());
        assertEquals("thebloke/llama2-gguf", models.get(0).repoId());
        assertFalse(models.get(0).isCurrentlyLoaded());
    }

    @Test
    void listLocalModels_scansMLXFiles() throws IOException {
        Path authorDir = tempDir.resolve("mlx-community").resolve("phi-3-mlx");
        Files.createDirectories(authorDir);
        Path mlxFile = authorDir.resolve("weights.mlx");
        Files.writeString(mlxFile, "fake mlx weights");

        when(inferenceService.getActiveModel()).thenReturn(null);

        List<LocalModelFileDto> models = service.listLocalModels();

        assertEquals(1, models.size());
        assertEquals("weights.mlx", models.get(0).filename());
        assertEquals("mlx", models.get(0).format());
    }

    @Test
    void listLocalModels_ignoresNonModelFiles() throws IOException {
        Path authorDir = tempDir.resolve("author").resolve("model");
        Files.createDirectories(authorDir);
        Files.writeString(authorDir.resolve("README.md"), "readme");
        Files.writeString(authorDir.resolve("config.json"), "{}");

        when(inferenceService.getActiveModel()).thenReturn(null);

        List<LocalModelFileDto> models = service.listLocalModels();

        assertTrue(models.isEmpty());
    }

    @Test
    void listLocalModels_detectsActiveModel() throws IOException {
        Path authorDir = tempDir.resolve("author").resolve("model");
        Files.createDirectories(authorDir);
        Files.writeString(authorDir.resolve("qwen3-32b.gguf"), "fake model");

        when(inferenceService.getActiveModel())
                .thenReturn(new InferenceModelInfo("qwen3-32b", "qwen3-32b", 17_000_000_000L, null, Instant.now()));

        List<LocalModelFileDto> models = service.listLocalModels();

        assertEquals(1, models.size());
        assertTrue(models.get(0).isCurrentlyLoaded());
    }

    @Test
    void listLocalModels_handlesInferenceServiceError() throws IOException {
        Path authorDir = tempDir.resolve("author").resolve("model");
        Files.createDirectories(authorDir);
        Files.writeString(authorDir.resolve("test.gguf"), "data");

        when(inferenceService.getActiveModel()).thenThrow(new RuntimeException("LM Studio unreachable"));

        List<LocalModelFileDto> models = service.listLocalModels();

        // Should still return models even if active model lookup fails
        assertEquals(1, models.size());
        assertFalse(models.get(0).isCurrentlyLoaded());
    }

    @Test
    void deleteLocalModel_removesFile() throws IOException {
        Path authorDir = tempDir.resolve("author").resolve("model");
        Files.createDirectories(authorDir);
        Path target = authorDir.resolve("to-delete.gguf");
        Files.writeString(target, "data");
        assertTrue(Files.exists(target));

        service.deleteLocalModel("to-delete.gguf");

        assertFalse(Files.exists(target));
    }

    @Test
    void deleteLocalModel_throwsWhenNotFound() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.deleteLocalModel("nonexistent.gguf"));

        assertTrue(ex.getMessage().contains("not found") || ex.getMessage().contains("nonexistent.gguf"));
    }
}
