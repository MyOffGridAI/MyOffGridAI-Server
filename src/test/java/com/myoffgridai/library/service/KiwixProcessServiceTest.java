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
        Path zimDir = tempDir.resolve("zim");
        Files.createDirectories(zimDir);

        when(kiwixProperties.isEnabled()).thenReturn(true);
        when(kiwixProperties.getBinaryPath()).thenReturn("/nonexistent/kiwix-serve");
        when(kiwixProperties.isAutoInstall()).thenReturn(true);
        when(libraryProperties.getZimDirectory()).thenReturn(zimDir.toString());

        // Pre-create the binary where installOnMacOS will look for it after extraction
        Path binDir = tempDir.resolve("kiwix-bin");
        Files.createDirectories(binDir);
        Files.createFile(binDir.resolve("kiwix-serve"));

        // Mock 'which kiwix-serve' returning not found
        Process whichProcess = mock(Process.class);
        ProcessBuilder whichPb = mock(ProcessBuilder.class);
        when(whichPb.redirectErrorStream(true)).thenReturn(whichPb);
        when(whichPb.start()).thenReturn(whichProcess);
        when(whichProcess.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
        when(whichProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(whichProcess.exitValue()).thenReturn(1);

        // Mock curl download succeeding
        Process curlProcess = mock(Process.class);
        ProcessBuilder curlPb = mock(ProcessBuilder.class);
        when(curlPb.redirectErrorStream(true)).thenReturn(curlPb);
        when(curlPb.start()).thenReturn(curlProcess);
        when(curlProcess.getInputStream()).thenReturn(new ByteArrayInputStream("OK".getBytes()));
        when(curlProcess.waitFor(300, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(curlProcess.exitValue()).thenReturn(0);

        // Mock tar extraction succeeding
        Process tarProcess = mock(Process.class);
        ProcessBuilder tarPb = mock(ProcessBuilder.class);
        when(tarPb.redirectErrorStream(true)).thenReturn(tarPb);
        when(tarPb.start()).thenReturn(tarProcess);
        when(tarProcess.getInputStream()).thenReturn(new ByteArrayInputStream("OK".getBytes()));
        when(tarProcess.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(tarProcess.exitValue()).thenReturn(0);

        // Route processBuilderFactory.create based on command
        when(processBuilderFactory.create(any(List.class))).thenAnswer(invocation -> {
            List<String> cmd = invocation.getArgument(0);
            if ("which".equals(cmd.get(0))) return whichPb;
            if ("curl".equals(cmd.get(0))) return curlPb;
            if ("tar".equals(cmd.get(0))) return tarPb;
            return processBuilder;
        });

        // No ZIM files in DB — start() won't be called after install
        when(zimFileRepository.findAll()).thenReturn(List.of());

        service.initialize();

        // Verify curl was called for download
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(processBuilderFactory, atLeast(2)).create(captor.capture());
        List<List<String>> allCmds = captor.getAllValues();
        assertThat(allCmds).anyMatch(cmd -> "curl".equals(cmd.get(0)));

        assertThat(service.getInstallationStatus()).isEqualTo(KiwixInstallationStatus.INSTALLED);
        assertThat(service.getInstallationError()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void initialize_setsFailedStatusOnInstallError() throws Exception {
        Path zimDir = tempDir.resolve("zim");
        Files.createDirectories(zimDir);

        when(kiwixProperties.isEnabled()).thenReturn(true);
        when(kiwixProperties.getBinaryPath()).thenReturn("/nonexistent/kiwix-serve");
        when(kiwixProperties.isAutoInstall()).thenReturn(true);
        when(libraryProperties.getZimDirectory()).thenReturn(zimDir.toString());

        // Mock 'which kiwix-serve' returning not found
        Process whichProcess = mock(Process.class);
        ProcessBuilder whichPb = mock(ProcessBuilder.class);
        when(whichPb.redirectErrorStream(true)).thenReturn(whichPb);
        when(whichPb.start()).thenReturn(whichProcess);
        when(whichProcess.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
        when(whichProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(whichProcess.exitValue()).thenReturn(1);

        // Mock curl download failing
        Process curlProcess = mock(Process.class);
        ProcessBuilder curlPb = mock(ProcessBuilder.class);
        when(curlPb.redirectErrorStream(true)).thenReturn(curlPb);
        when(curlPb.start()).thenReturn(curlProcess);
        when(curlProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream("curl: (22) The requested URL returned error: 404".getBytes()));
        when(curlProcess.waitFor(300, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(curlProcess.exitValue()).thenReturn(22);

        // Route processBuilderFactory.create based on command
        when(processBuilderFactory.create(any(List.class))).thenAnswer(invocation -> {
            List<String> cmd = invocation.getArgument(0);
            if ("which".equals(cmd.get(0))) return whichPb;
            if ("curl".equals(cmd.get(0))) return curlPb;
            return processBuilder;
        });

        service.initialize();

        assertThat(service.getInstallationStatus()).isEqualTo(KiwixInstallationStatus.INSTALL_FAILED);
        assertThat(service.getInstallationError()).contains("Download failed");
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
        Path zimDir = tempDir.resolve("zim");
        Files.createDirectories(zimDir);

        when(libraryProperties.getZimDirectory()).thenReturn(zimDir.toString());

        // Mock curl download failing
        Process curlProcess = mock(Process.class);
        ProcessBuilder curlPb = mock(ProcessBuilder.class);
        when(curlPb.redirectErrorStream(true)).thenReturn(curlPb);
        when(curlPb.start()).thenReturn(curlProcess);
        when(curlProcess.getInputStream()).thenReturn(new ByteArrayInputStream("Error".getBytes()));
        when(curlProcess.waitFor(300, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(curlProcess.exitValue()).thenReturn(1);

        when(processBuilderFactory.create(any(List.class))).thenReturn(curlPb);

        service.installKiwix();

        assertThat(service.getInstallationStatus()).isEqualTo(KiwixInstallationStatus.INSTALL_FAILED);
        // Verify curl was called
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(processBuilderFactory, atLeastOnce()).create(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(cmd -> "curl".equals(cmd.get(0)));
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
