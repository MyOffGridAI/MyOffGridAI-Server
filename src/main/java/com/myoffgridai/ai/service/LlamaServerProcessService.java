package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.LlamaServerStatusDto;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.config.InferenceProperties;
import com.myoffgridai.config.LlamaServerProperties;
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages the llama-server child process lifecycle.
 *
 * <p>Starts the Homebrew-installed {@code llama-server} binary as a child
 * process, monitors its health via HTTP polling, and restarts it on crash.
 * The active model is resolved from {@link SystemConfigService} or from
 * {@link LlamaServerProperties#getActiveModel()}.</p>
 *
 * <p>Activated when {@code app.inference.provider=llama-server}.</p>
 */
@Service
@ConditionalOnProperty(name = "app.inference.provider", havingValue = "llama-server")
public class LlamaServerProcessService implements ApplicationRunner, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(LlamaServerProcessService.class);
    private static final int LOG_BUFFER_MAX_LINES = 200;

    private final InferenceProperties inferenceProperties;
    private final LlamaServerProperties properties;
    private final SystemConfigService systemConfigService;
    private final ProcessBuilderFactory processBuilderFactory;

    private volatile Process process;
    private volatile LlamaServerStatus status = LlamaServerStatus.STOPPED;
    private volatile String errorMessage;
    private volatile String activeModelPath;

    private final LinkedList<String> logBuffer = new LinkedList<>();

    /**
     * Lifecycle states for the llama-server process.
     */
    public enum LlamaServerStatus {
        STOPPED, STARTING, RUNNING, ERROR
    }

    /**
     * Constructs the llama-server process service.
     *
     * @param inferenceProperties   inference configuration with process management flag
     * @param properties            llama-server configuration properties
     * @param systemConfigService   system config service for active model lookup
     * @param processBuilderFactory factory for creating ProcessBuilder instances
     */
    public LlamaServerProcessService(InferenceProperties inferenceProperties,
                                      LlamaServerProperties properties,
                                      SystemConfigService systemConfigService,
                                      ProcessBuilderFactory processBuilderFactory) {
        this.inferenceProperties = inferenceProperties;
        this.properties = properties;
        this.systemConfigService = systemConfigService;
        this.processBuilderFactory = processBuilderFactory;
    }

    /**
     * Called on application startup. Attempts to start llama-server with
     * the configured active model. Never propagates exceptions — startup
     * failures are logged at WARN level.
     *
     * @param args application arguments (unused)
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!inferenceProperties.isManageProcess()) {
            log.debug("llama-server process management disabled (app.inference.manage-process=false) — skipping");
            return;
        }
        try {
            start();
        } catch (Exception e) {
            log.warn("Failed to start llama-server on startup: {}", e.getMessage());
        }
    }

    /**
     * Starts the llama-server process with the active model.
     *
     * <p>Resolves the model filename from {@link SystemConfigService} first,
     * falling back to {@link LlamaServerProperties#getActiveModel()}. If no
     * model is configured, logs an INFO message and returns without starting.</p>
     *
     * <p>Verifies the binary exists and the model file is found in the models
     * directory (up to 3 subdirectory levels deep). After launch, polls the
     * {@code /health} endpoint until the server is healthy or the startup
     * timeout expires.</p>
     */
    public synchronized void start() {
        if (!inferenceProperties.isManageProcess()) {
            log.debug("llama-server process management disabled (app.inference.manage-process=false) — skipping");
            return;
        }
        if (status == LlamaServerStatus.RUNNING) {
            return;
        }

        // Resolve active model filename
        String modelFilename = systemConfigService.getConfig().getActiveModelFilename();
        if (modelFilename == null || modelFilename.isBlank()) {
            modelFilename = properties.getActiveModel();
        }
        if (modelFilename == null || modelFilename.isBlank()) {
            log.info("No active model configured — llama-server will not start");
            status = LlamaServerStatus.STOPPED;
            return;
        }

        // Resolve model path
        Path modelPath = resolveModelPath(modelFilename);
        if (modelPath == null) {
            String msg = "Model file not found: " + modelFilename
                    + " (searched in " + properties.getModelsDir() + ")";
            log.warn(msg);
            status = LlamaServerStatus.ERROR;
            errorMessage = msg;
            return;
        }

        // Verify binary exists
        String binaryPath = properties.getBinary();
        if (binaryPath == null || !Files.exists(Path.of(binaryPath))) {
            String msg = "llama-server binary not found at " + binaryPath
                    + " — install via: brew install llama.cpp";
            log.warn(msg);
            status = LlamaServerStatus.ERROR;
            errorMessage = msg;
            return;
        }

        // Build command
        List<String> command = List.of(
                binaryPath,
                "-m", modelPath.toString(),
                "--port", String.valueOf(properties.getPort()),
                "-c", String.valueOf(properties.getContextSize()),
                "-ngl", String.valueOf(properties.getGpuLayers()),
                "-t", String.valueOf(properties.getThreads()),
                "--host", "127.0.0.1",
                "--jinja"
        );

        try {
            ProcessBuilder pb = processBuilderFactory.create(command);
            pb.redirectErrorStream(true);
            process = pb.start();
            activeModelPath = modelPath.toString();
            status = LlamaServerStatus.STARTING;
            errorMessage = null;

            log.info("llama-server starting with PID {} — model: {} port: {}",
                    process.pid(), modelPath.getFileName(), properties.getPort());

            // Start log reader thread
            startLogReader();

            // Poll for health
            if (waitForHealthy()) {
                status = LlamaServerStatus.RUNNING;
                log.info("llama-server ready — model: {} port: {}",
                        modelPath.getFileName(), properties.getPort());
            } else {
                status = LlamaServerStatus.ERROR;
                errorMessage = "llama-server did not become healthy within "
                        + properties.getStartupTimeoutSeconds() + "s";
                log.warn(errorMessage);
            }

        } catch (IOException e) {
            status = LlamaServerStatus.ERROR;
            errorMessage = "Failed to start llama-server: " + e.getMessage();
            log.warn(errorMessage);
        }
    }

    /**
     * Stops the llama-server process gracefully, forcibly killing it if
     * it does not terminate within 10 seconds.
     */
    public synchronized void stop() {
        if (!inferenceProperties.isManageProcess()) {
            log.debug("llama-server process management disabled (app.inference.manage-process=false) — skipping");
            return;
        }
        if (process != null) {
            log.info("Stopping llama-server");
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
        status = LlamaServerStatus.STOPPED;
        log.info("llama-server stopped");
    }

    /**
     * Restarts the llama-server process by stopping and then starting it.
     */
    public void restart() {
        if (!inferenceProperties.isManageProcess()) {
            log.debug("llama-server process management disabled (app.inference.manage-process=false) — skipping");
            return;
        }
        stop();
        start();
    }

    /**
     * Switches the active model by stopping the current server, updating
     * the system config, and restarting with the new model.
     *
     * @param filename the GGUF model filename to switch to
     * @return the server status after the switch
     * @throws IllegalArgumentException if filename is blank or the file is not found
     */
    public synchronized LlamaServerStatusDto switchModel(String filename) {
        if (!inferenceProperties.isManageProcess()) {
            log.debug("llama-server process management disabled (app.inference.manage-process=false) — skipping");
            return getStatus();
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Model filename must not be blank");
        }

        Path modelPath = resolveModelPath(filename);
        if (modelPath == null) {
            throw new IllegalArgumentException("Model file not found: " + filename
                    + " (searched in " + properties.getModelsDir() + ")");
        }

        stop();
        systemConfigService.setActiveModelFilename(filename);
        start();

        return getStatus();
    }

    /**
     * Returns the current status of the llama-server process.
     *
     * @return a DTO with status, active model path, port, models dir, and error info
     */
    public LlamaServerStatusDto getStatus() {
        return new LlamaServerStatusDto(
                status.name(),
                activeModelPath,
                properties.getPort(),
                properties.getModelsDir(),
                errorMessage,
                status == LlamaServerStatus.RUNNING
        );
    }

    /**
     * Returns the most recent log lines from the llama-server process output.
     *
     * @param n the maximum number of lines to return
     * @return the last N log lines, or an empty list if the process is not running
     */
    public List<String> getRecentLogLines(int n) {
        synchronized (logBuffer) {
            if (logBuffer.isEmpty()) {
                return List.of();
            }
            int fromIndex = Math.max(0, logBuffer.size() - n);
            return new ArrayList<>(logBuffer.subList(fromIndex, logBuffer.size()));
        }
    }

    /**
     * Periodic health check that detects crashes and triggers automatic restarts.
     *
     * <p>When the server is in RUNNING state and becomes unreachable, it is
     * assumed to have crashed and is restarted. When in ERROR state with a
     * model configured, a recovery start is attempted.</p>
     */
    @Scheduled(fixedDelayString = "#{${app.llama-server.health-check-interval-seconds:30} * 1000}")
    public void monitorHealth() {
        if (!inferenceProperties.isManageProcess()) {
            log.debug("llama-server process management disabled (app.inference.manage-process=false) — skipping");
            return;
        }
        if (status == LlamaServerStatus.RUNNING) {
            if (!checkHealth()) {
                log.warn("llama-server appears to have crashed — restarting");
                restart();
            }
        } else if (status == LlamaServerStatus.ERROR) {
            String modelFilename = systemConfigService.getConfig().getActiveModelFilename();
            if (modelFilename != null && !modelFilename.isBlank()) {
                log.info("Attempting recovery start of llama-server");
                start();
            }
        }
    }

    /**
     * Terminates the llama-server process on application shutdown.
     */
    @Override
    public void destroy() {
        if (!inferenceProperties.isManageProcess()) {
            log.debug("llama-server process management disabled (app.inference.manage-process=false) — skipping");
            return;
        }
        stop();
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private Path resolveModelPath(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }

        // Try as absolute path first
        Path absolutePath = Path.of(filename);
        if (absolutePath.isAbsolute() && Files.isRegularFile(absolutePath)) {
            return absolutePath;
        }

        // Try as path relative to models dir (may include subdirectories)
        Path modelsDir = Path.of(properties.getModelsDir());
        Path relativePath = modelsDir.resolve(filename);
        if (Files.isRegularFile(relativePath)) {
            return relativePath;
        }

        // Walk models directory up to 3 levels to find the file
        if (!Files.exists(modelsDir)) {
            return null;
        }

        String leafFilename = Path.of(filename).getFileName().toString();
        try (var stream = Files.walk(modelsDir, 3)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(leafFilename))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            log.warn("Failed to search models directory: {}", e.getMessage());
            return null;
        }
    }

    private boolean waitForHealthy() {
        long deadlineMs = System.currentTimeMillis()
                + (long) properties.getStartupTimeoutSeconds() * 1000;

        while (System.currentTimeMillis() < deadlineMs) {
            if (process != null && !process.isAlive()) {
                log.warn("llama-server exited prematurely with code {}", process.exitValue());
                return false;
            }
            if (checkHealth()) {
                return true;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(AppConstants.LLAMA_SERVER_STARTUP_POLL_INTERVAL_MS);
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
                    "http://localhost:" + properties.getPort() + "/health"
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

    private void startLogReader() {
        if (process == null) {
            return;
        }
        Thread logThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (logBuffer) {
                        logBuffer.addLast(line);
                        while (logBuffer.size() > LOG_BUFFER_MAX_LINES) {
                            logBuffer.removeFirst();
                        }
                    }
                }
            } catch (IOException e) {
                // Process terminated — expected
            }
        }, "llama-server-log-reader");
        logThread.setDaemon(true);
        logThread.start();
    }
}
