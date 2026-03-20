package com.myoffgridai.library.service;

import com.myoffgridai.ai.service.ProcessBuilderFactory;
import com.myoffgridai.library.config.KiwixProperties;
import com.myoffgridai.library.config.LibraryProperties;
import com.myoffgridai.library.repository.ZimFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Manages the kiwix-serve process lifecycle (start, stop, restart).
 *
 * <p>Follows the {@code JudgeModelProcessService} pattern. Builds a command
 * from configured binary path, port, threads, and all ZIM file paths found
 * in the ZIM directory and database. Implements {@link DisposableBean} to
 * stop the process on application shutdown.</p>
 */
@Service
public class KiwixProcessService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(KiwixProcessService.class);

    private final KiwixProperties kiwixProperties;
    private final LibraryProperties libraryProperties;
    private final ZimFileRepository zimFileRepository;
    private final ProcessBuilderFactory processBuilderFactory;

    private volatile Process process;

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

        List<String> zimPaths = collectZimPaths();
        if (zimPaths.isEmpty()) {
            log.info("No ZIM files found — cannot start kiwix-serve");
            return;
        }

        String binary = kiwixProperties.getBinaryPath();
        if (binary == null || !Files.exists(Path.of(binary))) {
            log.warn("kiwix-serve binary not found: {} — cannot start kiwix process", binary);
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(binary);
        command.add("--port");
        command.add(String.valueOf(kiwixProperties.getPort()));
        command.add("--threads");
        command.add(String.valueOf(kiwixProperties.getThreads()));
        command.addAll(zimPaths);

        try {
            ProcessBuilder pb = processBuilderFactory.create(command);
            pb.redirectErrorStream(true);
            process = pb.start();
            log.info("kiwix-serve started with PID {} — serving {} ZIM files", process.pid(), zimPaths.size());

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
                    "http://localhost:" + kiwixProperties.getPort() + "/"
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
