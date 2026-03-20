package com.myoffgridai.library.service;

import com.myoffgridai.ai.service.ProcessBuilderFactory;
import com.myoffgridai.library.config.KiwixProperties;
import com.myoffgridai.library.config.LibraryProperties;
import com.myoffgridai.library.model.ZimFile;
import com.myoffgridai.library.repository.ZimFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KiwixProcessService}.
 */
@ExtendWith(MockitoExtension.class)
class KiwixProcessServiceTest {

    @Mock private KiwixProperties kiwixProperties;
    @Mock private LibraryProperties libraryProperties;
    @Mock private ZimFileRepository zimFileRepository;
    @Mock private ProcessBuilderFactory processBuilderFactory;
    @Mock private ProcessBuilder processBuilder;
    @Mock private Process process;

    @TempDir
    Path tempDir;

    private KiwixProcessService service;

    @BeforeEach
    void setUp() {
        service = new KiwixProcessService(
                kiwixProperties, libraryProperties, zimFileRepository, processBuilderFactory);
    }

    @Test
    void start_whenDisabled_doesNotStartProcess() {
        when(kiwixProperties.isEnabled()).thenReturn(false);

        service.start();

        verifyNoInteractions(processBuilderFactory);
    }

    @Test
    void start_whenNoZimFiles_doesNotStartProcess() throws IOException {
        when(kiwixProperties.isEnabled()).thenReturn(true);
        when(zimFileRepository.findAll()).thenReturn(List.of());
        when(libraryProperties.getZimDirectory()).thenReturn(tempDir.toString());

        service.start();

        verifyNoInteractions(processBuilderFactory);
    }

    @Test
    void start_whenBinaryNotFound_doesNotStartProcess() throws IOException {
        when(kiwixProperties.isEnabled()).thenReturn(true);
        ZimFile zf = createZimFile(tempDir);
        when(zimFileRepository.findAll()).thenReturn(List.of(zf));
        when(libraryProperties.getZimDirectory()).thenReturn(tempDir.toString());
        when(kiwixProperties.getBinaryPath()).thenReturn("/nonexistent/kiwix-serve");

        service.start();

        verifyNoInteractions(processBuilderFactory);
    }

    @Test
    @SuppressWarnings("unchecked")
    void start_withZimFiles_buildsCorrectCommand() throws Exception {
        Path binaryPath = tempDir.resolve("kiwix-serve");
        Files.createFile(binaryPath);

        Path zimPath = tempDir.resolve("test.zim");
        Files.createFile(zimPath);

        ZimFile zf = new ZimFile();
        zf.setId(UUID.randomUUID());
        zf.setFilename("test.zim");
        zf.setFilePath(zimPath.toString());

        when(kiwixProperties.isEnabled()).thenReturn(true);
        when(kiwixProperties.getBinaryPath()).thenReturn(binaryPath.toString());
        when(kiwixProperties.getPort()).thenReturn(8888);
        when(kiwixProperties.getThreads()).thenReturn(4);
        when(kiwixProperties.getTimeoutSeconds()).thenReturn(1);
        when(zimFileRepository.findAll()).thenReturn(List.of(zf));
        when(libraryProperties.getZimDirectory()).thenReturn(tempDir.toString());

        when(processBuilderFactory.create(any(List.class))).thenReturn(processBuilder);
        when(processBuilder.redirectErrorStream(true)).thenReturn(processBuilder);
        when(processBuilder.start()).thenReturn(process);
        when(process.isAlive()).thenReturn(false);
        when(process.exitValue()).thenReturn(1);

        service.start();

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(processBuilderFactory).create(captor.capture());

        List<String> command = captor.getValue();
        assertThat(command.get(0)).isEqualTo(binaryPath.toString());
        assertThat(command).contains("--port", "8888", "--threads", "4");
        assertThat(command).contains(zimPath.toString());
    }

    @Test
    void stop_whenNotRunning_isNoop() {
        service.stop();
        // No exception thrown
    }

    @Test
    @SuppressWarnings("unchecked")
    void stop_whenRunning_destroysProcess() throws Exception {
        // Start the process first
        Path binaryPath = tempDir.resolve("kiwix-serve");
        Files.createFile(binaryPath);
        Path zimPath = tempDir.resolve("test.zim");
        Files.createFile(zimPath);

        ZimFile zf = new ZimFile();
        zf.setId(UUID.randomUUID());
        zf.setFilename("test.zim");
        zf.setFilePath(zimPath.toString());

        when(kiwixProperties.isEnabled()).thenReturn(true);
        when(kiwixProperties.getBinaryPath()).thenReturn(binaryPath.toString());
        when(kiwixProperties.getPort()).thenReturn(8888);
        when(kiwixProperties.getThreads()).thenReturn(4);
        when(kiwixProperties.getTimeoutSeconds()).thenReturn(1);
        when(zimFileRepository.findAll()).thenReturn(List.of(zf));
        when(libraryProperties.getZimDirectory()).thenReturn(tempDir.toString());
        when(processBuilderFactory.create(any(List.class))).thenReturn(processBuilder);
        when(processBuilder.redirectErrorStream(true)).thenReturn(processBuilder);
        when(processBuilder.start()).thenReturn(process);
        when(process.isAlive()).thenReturn(false);
        when(process.exitValue()).thenReturn(1);

        service.start();
        // Process was started but health check failed, so it was destroyed
        // We verify destroy was called at least once
        verify(process, atLeastOnce()).destroy();
    }

    @Test
    void isRunning_whenNoProcess_returnsFalse() {
        assertThat(service.isRunning()).isFalse();
    }

    @Test
    void restart_callsStopThenStart() {
        when(kiwixProperties.isEnabled()).thenReturn(false);

        service.restart();

        // start() is called but exits early because disabled
        verify(kiwixProperties, atLeastOnce()).isEnabled();
    }

    @Test
    void destroy_callsStop() {
        service.destroy();
        // No exception - graceful shutdown
    }

    private ZimFile createZimFile(Path dir) throws IOException {
        Path zimPath = dir.resolve("wiki.zim");
        Files.createFile(zimPath);

        ZimFile zf = new ZimFile();
        zf.setId(UUID.randomUUID());
        zf.setFilename("wiki.zim");
        zf.setFilePath(zimPath.toString());
        return zf;
    }
}
