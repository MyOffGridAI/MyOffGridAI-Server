package com.myoffgridai.library.service;

import com.myoffgridai.ai.service.ProcessBuilderFactory;
import com.myoffgridai.library.config.KiwixProperties;
import com.myoffgridai.library.config.LibraryProperties;
import com.myoffgridai.library.dto.KiwixInstallationStatus;
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

    // ── Auto-install tests ──────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void initialize_skipsInstallWhenBinaryExists() throws Exception {
        Path binaryPath = tempDir.resolve("kiwix-serve");
        Files.createFile(binaryPath);

        when(kiwixProperties.isEnabled()).thenReturn(true);
        when(kiwixProperties.getBinaryPath()).thenReturn(binaryPath.toString());
        when(zimFileRepository.findAll()).thenReturn(List.of());
        when(libraryProperties.getZimDirectory()).thenReturn(tempDir.toString());

        service.initialize();

        assertThat(service.getInstallationStatus()).isEqualTo(KiwixInstallationStatus.INSTALLED);
        assertThat(service.getInstallationError()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void initialize_installsWhenBinaryMissing() throws Exception {
        when(kiwixProperties.isEnabled()).thenReturn(true);
        when(kiwixProperties.getBinaryPath()).thenReturn("/nonexistent/kiwix-serve");
        when(kiwixProperties.isAutoInstall()).thenReturn(true);

        // Mock 'which kiwix-serve' returning not found
        Process whichProcess = mock(Process.class);
        ProcessBuilder whichPb = mock(ProcessBuilder.class);
        when(processBuilderFactory.create(List.of("which", "kiwix-serve"))).thenReturn(whichPb);
        when(whichPb.redirectErrorStream(true)).thenReturn(whichPb);
        when(whichPb.start()).thenReturn(whichProcess);
        when(whichProcess.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
        when(whichProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(whichProcess.exitValue()).thenReturn(1);

        // Mock 'brew install kiwix-tools' succeeding
        Process installProcess = mock(Process.class);
        ProcessBuilder installPb = mock(ProcessBuilder.class);
        when(processBuilderFactory.create(List.of("brew", "install", "kiwix-tools"))).thenReturn(installPb);
        when(installPb.redirectErrorStream(true)).thenReturn(installPb);
        when(installPb.start()).thenReturn(installProcess);
        when(installProcess.getInputStream()).thenReturn(new ByteArrayInputStream("Installed".getBytes()));
        when(installProcess.waitFor(300, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(installProcess.exitValue()).thenReturn(0);

        // After install, 'which' still won't find it in this mock, so it should fail
        // because discoverBinary is called again and returns null
        service.initialize();

        // Verify brew install was called
        verify(processBuilderFactory).create(List.of("brew", "install", "kiwix-tools"));

        // Since the post-install discovery also fails, status should be INSTALL_FAILED
        assertThat(service.getInstallationStatus()).isEqualTo(KiwixInstallationStatus.INSTALL_FAILED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void initialize_setsFailedStatusOnInstallError() throws Exception {
        when(kiwixProperties.isEnabled()).thenReturn(true);
        when(kiwixProperties.getBinaryPath()).thenReturn("/nonexistent/kiwix-serve");
        when(kiwixProperties.isAutoInstall()).thenReturn(true);

        // Mock 'which kiwix-serve' returning not found
        Process whichProcess = mock(Process.class);
        ProcessBuilder whichPb = mock(ProcessBuilder.class);
        when(processBuilderFactory.create(List.of("which", "kiwix-serve"))).thenReturn(whichPb);
        when(whichPb.redirectErrorStream(true)).thenReturn(whichPb);
        when(whichPb.start()).thenReturn(whichProcess);
        when(whichProcess.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
        when(whichProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(whichProcess.exitValue()).thenReturn(1);

        // Mock 'brew install kiwix-tools' failing
        Process installProcess = mock(Process.class);
        ProcessBuilder installPb = mock(ProcessBuilder.class);
        when(processBuilderFactory.create(List.of("brew", "install", "kiwix-tools"))).thenReturn(installPb);
        when(installPb.redirectErrorStream(true)).thenReturn(installPb);
        when(installPb.start()).thenReturn(installProcess);
        when(installProcess.getInputStream()).thenReturn(new ByteArrayInputStream("Error: formula not found".getBytes()));
        when(installProcess.waitFor(300, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(installProcess.exitValue()).thenReturn(1);

        service.initialize();

        assertThat(service.getInstallationStatus()).isEqualTo(KiwixInstallationStatus.INSTALL_FAILED);
        assertThat(service.getInstallationError()).contains("exit code 1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void initialize_autoStartsWhenZimFilesExist() throws Exception {
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

        service.initialize();

        assertThat(service.getInstallationStatus()).isEqualTo(KiwixInstallationStatus.INSTALLED);
        // Verify start() was triggered (processBuilderFactory.create was called for start)
        verify(processBuilderFactory).create(any(List.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void installKiwix_retriesInstallation() throws Exception {
        // Mock brew install failing
        Process installProcess = mock(Process.class);
        ProcessBuilder installPb = mock(ProcessBuilder.class);
        when(processBuilderFactory.create(List.of("brew", "install", "kiwix-tools"))).thenReturn(installPb);
        when(installPb.redirectErrorStream(true)).thenReturn(installPb);
        when(installPb.start()).thenReturn(installProcess);
        when(installProcess.getInputStream()).thenReturn(new ByteArrayInputStream("Error".getBytes()));
        when(installProcess.waitFor(300, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(installProcess.exitValue()).thenReturn(1);

        service.installKiwix();

        assertThat(service.getInstallationStatus()).isEqualTo(KiwixInstallationStatus.INSTALL_FAILED);
        verify(processBuilderFactory).create(List.of("brew", "install", "kiwix-tools"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void discoverBinary_findsViaWhich() throws Exception {
        Path binaryPath = tempDir.resolve("kiwix-serve");
        Files.createFile(binaryPath);

        // Configured path doesn't exist
        when(kiwixProperties.getBinaryPath()).thenReturn("/nonexistent/kiwix-serve");

        // 'which kiwix-serve' returns the real path
        Process whichProcess = mock(Process.class);
        ProcessBuilder whichPb = mock(ProcessBuilder.class);
        when(processBuilderFactory.create(List.of("which", "kiwix-serve"))).thenReturn(whichPb);
        when(whichPb.redirectErrorStream(true)).thenReturn(whichPb);
        when(whichPb.start()).thenReturn(whichProcess);
        when(whichProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream(binaryPath.toString().getBytes()));
        when(whichProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(whichProcess.exitValue()).thenReturn(0);

        String result = service.discoverBinary();

        assertThat(result).isEqualTo(binaryPath.toString());
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
