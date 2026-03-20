package com.myoffgridai.library.service;

import com.myoffgridai.ai.service.ProcessBuilderFactory;
import com.myoffgridai.library.config.KiwixProperties;
import com.myoffgridai.library.config.LibraryProperties;
import com.myoffgridai.library.dto.KiwixInstallationStatus;
import com.myoffgridai.library.repository.ZimFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Manages the kiwix-serve process lifecycle (start, stop, restart)
 * and auto-installation of kiwix-tools.
 *
 * <p>On application startup, checks for the kiwix-serve binary and
 * optionally auto-installs it via the system package manager.
 * Implements {@link DisposableBean} to stop the process on shutdown.</p>
 */
@Service
public class KiwixProcessService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(KiwixProcessService.class);

    private final KiwixProperties kiwixProperties;
    private final LibraryProperties libraryProperties;
    private final ZimFileRepository zimFileRepository;
    private final ProcessBuilderFactory processBuilderFactory;

    private volatile Process process;
    private volatile KiwixInstallationStatus installationStatus = KiwixInstallationStatus.NOT_CHECKED;
    private volatile String installationError = null;

    /**
     * Constructs the Kiwix process service.
     *
     * @param kiwixProperties       kiwix-serve configuration
     * @param libraryProperties     library configuration (ZIM directory)
     * @param zimFileRepository     repository for ZIM file entities
     * @param processBuilderFactory factory for creating ProcessBuilder instances
     */
    public KiwixProcessService(KiwixProperties kiwixProperties,
                                LibraryProperties libraryProperties,
                                ZimFileRepository zimFileRepository,
                                ProcessBuilderFactory processBuilderFactory) {
        this.kiwixProperties = kiwixProperties;
        this.libraryProperties = libraryProperties;
        this.zimFileRepository = zimFileRepository;
        this.processBuilderFactory = processBuilderFactory;
    }

    /**
     * Post-startup initialization: discovers or installs kiwix-serve,
     * then auto-starts if ZIM files exist.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initialize() {
        if (!kiwixProperties.isEnabled()) {
            log.info("Kiwix process management is disabled — skipping initialization");
            installationStatus = KiwixInstallationStatus.NOT_INSTALLED;
            return;
        }

        installationStatus = KiwixInstallationStatus.CHECKING;
        log.info("Checking for kiwix-serve binary...");

        String discovered = discoverBinary();
        if (discovered != null) {
            kiwixProperties.setBinaryPath(discovered);
            installationStatus = KiwixInstallationStatus.INSTALLED;
            log.info("kiwix-serve found at: {}", discovered);
        } else if (kiwixProperties.isAutoInstall()) {
            autoInstallBinary();
        } else {
            installationStatus = KiwixInstallationStatus.NOT_INSTALLED;
            log.warn("kiwix-serve not found and auto-install is disabled");
        }

        if (installationStatus == KiwixInstallationStatus.INSTALLED) {
            List<String> zimPaths = collectZimPaths();
            if (!zimPaths.isEmpty()) {
                log.info("ZIM files found — auto-starting kiwix-serve");
                start();
            } else {
                log.info("No ZIM files found — kiwix-serve installed but not started");
            }
        }
    }

    /**
     * Manually triggers kiwix-tools installation (retry after failure).
     */
    public void installKiwix() {
        autoInstallBinary();
        if (installationStatus == KiwixInstallationStatus.INSTALLED) {
            List<String> zimPaths = collectZimPaths();
            if (!zimPaths.isEmpty()) {
                start();
            }
        }
    }

    /**
     * Returns the current installation status.
     *
     * @return the installation status enum value
     */
    public KiwixInstallationStatus getInstallationStatus() {
        return installationStatus;
    }

    /**
     * Returns the installation error message, if any.
     *
     * @return the error message or null
     */
    public String getInstallationError() {
        return installationError;
    }

    /**
     * Starts the kiwix-serve process if not already running.
     *
     * <p>No-op if kiwix is disabled, the process is already running,
     * no ZIM files exist, or the binary is not found.</p>
     */
    public synchronized void start() {
        if (!kiwixProperties.isEnabled()) {
            log.info("Kiwix process management is disabled — skipping start");
            return;
        }

        if (process != null && process.isAlive()) {
            log.info("Kiwix process already running — ignoring start request");
            return;
        }

        String binary = kiwixProperties.getBinaryPath();
        if (binary == null || !Files.exists(Path.of(binary))) {
            log.warn("kiwix-serve binary not found: {} — cannot start kiwix process", binary);
            return;
        }

        List<String> zimPaths = collectZimPaths();
        if (zimPaths.isEmpty()) {
            log.info("No ZIM files found — cannot start kiwix-serve");
            return;
        }

        // Validate each ZIM by attempting a dry-run with kiwix-serve.
        // Some ZIM files use a format version newer than the installed binary
        // and cause kiwix-serve to exit immediately with code 1.
        List<String> validPaths = new ArrayList<>();
        for (String zimPath : zimPaths) {
            if (validateZimFile(binary, zimPath)) {
                validPaths.add(zimPath);
            } else {
                log.warn("Skipping incompatible ZIM file: {}", zimPath);
            }
        }

        if (validPaths.isEmpty()) {
            log.warn("No compatible ZIM files found — cannot start kiwix-serve");
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(binary);
        command.add("--port");
        command.add(String.valueOf(kiwixProperties.getPort()));
        command.add("--threads");
        command.add(String.valueOf(kiwixProperties.getThreads()));
        command.addAll(validPaths);

        try {
            ProcessBuilder pb = processBuilderFactory.create(command);
            pb.redirectErrorStream(true);
            process = pb.start();
            log.info("kiwix-serve started with PID {} — serving {} ZIM files", process.pid(), zimPaths.size());

            // Capture process output in a background thread for diagnostics
            final Process proc = process;
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("kiwix-serve: {}", line);
                    }
                } catch (IOException e) {
                    log.debug("kiwix-serve output stream closed: {}", e.getMessage());
                }
            }, "kiwix-output-reader");
            outputReader.setDaemon(true);
            outputReader.start();

            if (waitForHealthy()) {
                log.info("kiwix-serve is healthy on port {}", kiwixProperties.getPort());
            } else {
                log.warn("kiwix-serve did not become healthy within timeout — stopping");
                destroyProcess();
            }
        } catch (IOException e) {
            log.error("Failed to start kiwix-serve: {}", e.getMessage(), e);
        }
    }

    /**
     * Stops the kiwix-serve process if running.
     */
    public synchronized void stop() {
        if (process == null) {
            return;
        }
        log.info("Stopping kiwix-serve process");
        destroyProcess();
        log.info("kiwix-serve stopped");
    }

    /**
     * Restarts the kiwix-serve process (stop then start).
     *
     * <p>Called when ZIM files are added or removed so that
     * kiwix-serve picks up the new content.</p>
     */
    public synchronized void restart() {
        log.info("Restarting kiwix-serve");
        stop();
        start();
    }

    /**
     * Returns whether the kiwix process is alive and responding to health checks.
     *
     * @return true if the process is alive and the root URL returns 200
     */
    public boolean isRunning() {
        return process != null && process.isAlive() && checkHealth();
    }

    /**
     * Returns the HTTP port kiwix-serve listens on.
     *
     * @return the kiwix port
     */
    public int getPort() {
        return kiwixProperties.getPort();
    }

    /**
     * Terminates the kiwix process on application shutdown.
     */
    @Override
    public void destroy() {
        stop();
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Discovers the kiwix-serve binary by checking the configured path
     * and then falling back to {@code which kiwix-serve}.
     *
     * @return the absolute path to the binary, or null if not found
     */
    String discoverBinary() {
        String configured = kiwixProperties.getBinaryPath();
        if (configured != null && Files.exists(Path.of(configured))) {
            return configured;
        }

        try {
            ProcessBuilder pb = processBuilderFactory.create(List.of("which", "kiwix-serve"));
            pb.redirectErrorStream(true);
            Process whichProcess = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(whichProcess.getInputStream()))) {
                String line = reader.readLine();
                boolean finished = whichProcess.waitFor(5, TimeUnit.SECONDS);
                if (finished && whichProcess.exitValue() == 0 && line != null && !line.isBlank()) {
                    String path = line.trim();
                    if (Files.exists(Path.of(path))) {
                        return path;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to run 'which kiwix-serve': {}", e.getMessage());
        }

        return null;
    }

    /**
     * Attempts to install kiwix-tools by downloading the binary from kiwix.org
     * (macOS) or via apt-get (Linux).
     */
    void autoInstallBinary() {
        installationStatus = KiwixInstallationStatus.INSTALLING;
        installationError = null;

        String osName = System.getProperty("os.name", "").toLowerCase();

        try {
            if (osName.contains("mac")) {
                installOnMacOS();
            } else if (osName.contains("linux")) {
                installViaCommand(List.of("apt-get", "install", "-y", "kiwix-tools"));
            } else {
                installationStatus = KiwixInstallationStatus.INSTALL_FAILED;
                installationError = "Unsupported platform: " + osName + ". Please install kiwix-tools manually.";
                log.error("Cannot auto-install kiwix-tools: {}", installationError);
                return;
            }
        } catch (Exception e) {
            installationStatus = KiwixInstallationStatus.INSTALL_FAILED;
            installationError = "Installation failed: " + e.getMessage();
            log.error("Failed to install kiwix-tools: {}", e.getMessage(), e);
        }
    }

    /**
     * Downloads kiwix-tools binary from download.kiwix.org for macOS.
     */
    private void installOnMacOS() throws Exception {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        String archSuffix = arch.contains("aarch64") || arch.contains("arm") ? "arm64" : "x86_64";
        String version = "3.8.2";
        String tarballName = "kiwix-tools_macos-" + archSuffix + "-" + version + ".tar.gz";
        String downloadUrl = "https://download.kiwix.org/release/kiwix-tools/" + tarballName;

        Path installDir = Path.of(libraryProperties.getZimDirectory()).getParent();
        if (installDir == null) {
            installDir = Path.of(libraryProperties.getZimDirectory());
        }
        Path binDir = installDir.resolve("kiwix-bin");
        Files.createDirectories(binDir);

        Path tarball = binDir.resolve(tarballName);

        log.info("Downloading kiwix-tools from: {}", downloadUrl);

        // Download the tarball
        List<String> curlCmd = List.of("curl", "-fSL", "-o", tarball.toString(), downloadUrl);
        ProcessBuilder curlPb = processBuilderFactory.create(curlCmd);
        curlPb.redirectErrorStream(true);
        Process curlProcess = curlPb.start();
        String curlOutput = readProcessOutput(curlProcess);
        boolean curlFinished = curlProcess.waitFor(300, TimeUnit.SECONDS);
        if (!curlFinished || curlProcess.exitValue() != 0) {
            installationStatus = KiwixInstallationStatus.INSTALL_FAILED;
            installationError = "Download failed: " + curlOutput;
            log.error("kiwix-tools download failed: {}", curlOutput);
            return;
        }

        // Extract the tarball
        log.info("Extracting kiwix-tools to: {}", binDir);
        List<String> tarCmd = List.of("tar", "xzf", tarball.toString(), "-C", binDir.toString(), "--strip-components=1");
        ProcessBuilder tarPb = processBuilderFactory.create(tarCmd);
        tarPb.redirectErrorStream(true);
        Process tarProcess = tarPb.start();
        String tarOutput = readProcessOutput(tarProcess);
        boolean tarFinished = tarProcess.waitFor(60, TimeUnit.SECONDS);
        if (!tarFinished || tarProcess.exitValue() != 0) {
            installationStatus = KiwixInstallationStatus.INSTALL_FAILED;
            installationError = "Extraction failed: " + tarOutput;
            log.error("kiwix-tools extraction failed: {}", tarOutput);
            return;
        }

        // Clean up tarball
        Files.deleteIfExists(tarball);

        // Verify binary exists
        Path binaryPath = binDir.resolve("kiwix-serve");
        if (Files.exists(binaryPath)) {
            kiwixProperties.setBinaryPath(binaryPath.toString());
            installationStatus = KiwixInstallationStatus.INSTALLED;
            installationError = null;
            log.info("kiwix-tools installed successfully — binary at: {}", binaryPath);
        } else {
            installationStatus = KiwixInstallationStatus.INSTALL_FAILED;
            installationError = "Download succeeded but kiwix-serve binary not found in extracted files";
            log.error("kiwix-serve not found after extraction in: {}", binDir);
        }
    }

    /**
     * Installs kiwix-tools via a system package manager command (e.g., apt-get).
     */
    private void installViaCommand(List<String> installCommand) throws Exception {
        log.info("Installing kiwix-tools via: {}", String.join(" ", installCommand));

        ProcessBuilder pb = processBuilderFactory.create(installCommand);
        pb.redirectErrorStream(true);
        Process installProcess = pb.start();
        String output = readProcessOutput(installProcess);

        boolean finished = installProcess.waitFor(300, TimeUnit.SECONDS);
        if (!finished) {
            installProcess.destroyForcibly();
            installationStatus = KiwixInstallationStatus.INSTALL_FAILED;
            installationError = "Installation timed out after 5 minutes";
            log.error("kiwix-tools installation timed out");
            return;
        }

        if (installProcess.exitValue() != 0) {
            installationStatus = KiwixInstallationStatus.INSTALL_FAILED;
            installationError = "Installation failed (exit code " + installProcess.exitValue() + "): "
                    + output.trim();
            log.error("kiwix-tools installation failed: {}", installationError);
            return;
        }

        String discovered = discoverBinary();
        if (discovered != null) {
            kiwixProperties.setBinaryPath(discovered);
            installationStatus = KiwixInstallationStatus.INSTALLED;
            installationError = null;
            log.info("kiwix-tools installed successfully — binary at: {}", discovered);
        } else {
            installationStatus = KiwixInstallationStatus.INSTALL_FAILED;
            installationError = "Installation command succeeded but kiwix-serve binary not found on PATH";
            log.error("kiwix-tools install completed but binary not discoverable");
        }
    }

    /**
     * Reads all output from a process into a string.
     */
    private String readProcessOutput(Process process) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
                log.debug("kiwix-tools install: {}", line);
            }
        } catch (IOException e) {
            log.debug("Error reading process output: {}", e.getMessage());
        }
        return sb.toString();
    }

    /**
     * Validates a ZIM file by attempting a quick kiwix-serve dry-run.
     *
     * <p>Starts kiwix-serve on an ephemeral port with just this ZIM file
     * and checks whether the process stays alive for 2 seconds. If it exits
     * immediately with a non-zero code, the ZIM is incompatible.</p>
     *
     * @param binary  path to the kiwix-serve binary
     * @param zimPath path to the ZIM file to validate
     * @return true if the ZIM is loadable by kiwix-serve
     */
    private boolean validateZimFile(String binary, String zimPath) {
        try {
            // Use a random high port for validation
            int testPort = 19000 + (int) (Math.random() * 1000);
            List<String> cmd = List.of(binary, "--port", String.valueOf(testPort), "--threads", "1", zimPath);
            ProcessBuilder pb = processBuilderFactory.create(cmd);
            pb.redirectErrorStream(true);
            Process testProcess = pb.start();

            // Give it 2 seconds to either start or crash
            boolean exited = testProcess.waitFor(2, TimeUnit.SECONDS);
            if (exited && testProcess.exitValue() != 0) {
                // Read output for diagnostics
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(testProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.warn("ZIM validation [{}]: {}", zimPath, line);
                    }
                }
                return false;
            }

            // Process is still running or exited successfully — ZIM is valid
            testProcess.destroyForcibly();
            testProcess.waitFor(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.warn("Failed to validate ZIM file {}: {}", zimPath, e.getMessage());
            return false;
        }
    }

    /**
     * Collects all ZIM file paths from both the database and the ZIM directory on disk.
     */
    private List<String> collectZimPaths() {
        List<String> paths = new ArrayList<>();

        // Collect paths from database entities
        zimFileRepository.findAll().forEach(zf -> {
            if (zf.getFilePath() != null && Files.exists(Path.of(zf.getFilePath()))) {
                paths.add(zf.getFilePath());
            }
        });

        // Also scan ZIM directory for files not yet in database
        Path zimDir = Path.of(libraryProperties.getZimDirectory());
        if (Files.exists(zimDir)) {
            try (Stream<Path> stream = Files.walk(zimDir, 1)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".zim"))
                        .map(Path::toString)
                        .filter(p -> !paths.contains(p))
                        .forEach(paths::add);
            } catch (IOException e) {
                log.warn("Failed to scan ZIM directory: {}", e.getMessage());
            }
        }

        return paths;
    }

    private boolean waitForHealthy() {
        long deadline = System.currentTimeMillis()
                + (long) kiwixProperties.getTimeoutSeconds() * 1000;

        while (System.currentTimeMillis() < deadline) {
            if (process != null && !process.isAlive()) {
                log.error("kiwix-serve exited prematurely with code {}", process.exitValue());
                return false;
            }
            if (checkHealth()) {
                return true;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean checkHealth() {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(
                    "http://127.0.0.1:" + kiwixProperties.getPort() + "/"
            ).toURL().openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private void destroyProcess() {
        if (process != null) {
            process.destroy();
            try {
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            process = null;
        }
    }
}
