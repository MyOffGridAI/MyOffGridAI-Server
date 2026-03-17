package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.LlamaServerStatusDto;
import com.myoffgridai.config.AppConstants;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Manages the lifecycle of a llama-server (llama.cpp) child process.
 *
 * <p>Implements {@link ApplicationRunner} to auto-start the process on boot
 * and {@link DisposableBean} to gracefully stop it on shutdown. A scheduled
 * health monitor detects crashes and triggers automatic restarts.</p>
 *
 * <p>Only active when {@code app.inference.provider=llama-server}.</p>
 */
@Service
@ConditionalOnProperty(name = "app.inference.provider", havingValue = "llama-server")
public class LlamaServerProcessService implements ApplicationRunner, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(LlamaServerProcessService.class);

    private final LlamaServerProperties properties;
    private final SystemConfigService systemConfigService;
    private final ProcessBuilderFactory processBuilderFactory;
    private final InferenceService inferenceService;

    private volatile Process process;
    private volatile LlamaServerStatus status = LlamaServerStatus.STOPPED;
    private volatile String activeModelPath;
    private volatile String errorMessage;
    private final ArrayDeque<String> logBuffer = new ArrayDeque<>();

    /**
     * Constructs the llama-server process service.
     *
     * @param properties           llama-server configuration properties
     * @param systemConfigService  system config service for active model lookup
     * @param processBuilderFactory factory for creating ProcessBuilder instances
     * @param inferenceService     inference service for health polling
     */
    public LlamaServerProcessService(LlamaServerProperties properties,
                                      SystemConfigService systemConfigService,
                                      ProcessBuilderFactory processBuilderFactory,
                                      InferenceService inferenceService) {
        this.properties = properties;
        this.systemConfigService = systemConfigService;
        this.processBuilderFactory = processBuilderFactory;
        this.inferenceService = inferenceService;
    }

    /**
     * Auto-starts the llama-server process when the application boots.
     *
     * @param args application arguments (unused)
     */
    @Override
    public void run(ApplicationArguments args) {
        start();
    }

    /**
     * Gracefully stops the llama-server process on application shutdown.
     */
    @Override
    public void destroy() {
        stop();
    }

    /**
     * Starts the llama-server process with the resolved active model.
     *
     * <p>Resolves the model path from: (1) SystemConfig.activeModelFilename,
     * (2) LlamaServerProperties.activeModel, (3) empty → STOPPED.</p>
     */
    public synchronized void start() {
        if (status == LlamaServerStatus.STARTING || status == LlamaServerStatus.RUNNING) {
            log.info("llama-server already {} — ignoring start request", status);
            return;
        }

        String modelFilename = resolveActiveModelFilename();
        if (modelFilename == null || modelFilename.isBlank()) {
            log.info("No active model configured — llama-server will not start");
            status = LlamaServerStatus.STOPPED;
            activeModelPath = null;
            return;
        }

        Path modelPath = resolveModelPath(modelFilename);
        if (modelPath == null) {
            errorMessage = "Model file not found: " + modelFilename;
            log.error(errorMessage);
            status = LlamaServerStatus.ERROR;
            return;
        }

        String binary = properties.getLlamaServerBinary();
        if (binary == null || !Files.exists(Path.of(binary))) {
            errorMessage = "llama-server binary not found: " + binary;
            log.error(errorMessage);
            status = LlamaServerStatus.ERROR;
            return;
        }

        status = LlamaServerStatus.STARTING;
        errorMessage = null;
        activeModelPath = modelPath.toString();

        List<String> command = List.of(
                binary,
                "-m", modelPath.toString(),
                "--port", String.valueOf(properties.getPort()),
                "-c", String.valueOf(properties.getContextSize()),
                "-ngl", String.valueOf(properties.getGpuLayers()),
                "-t", String.valueOf(properties.getThreads()),
                "--host", "127.0.0.1"
        );

        try {
            ProcessBuilder pb = processBuilderFactory.create(command);
            pb.redirectErrorStream(true);
            process = pb.start();
            log.info("llama-server started with PID {} — model: {}", process.pid(), modelPath);

            startLogCapture(process);

            if (waitForHealthy()) {
                status = LlamaServerStatus.RUNNING;
                log.info("llama-server is healthy and accepting requests");
            } else {
                errorMessage = "llama-server failed to become healthy within timeout";
                log.error(errorMessage);
                status = LlamaServerStatus.ERROR;
                destroyProcess();
            }

        } catch (IOException e) {
            errorMessage = "Failed to start llama-server: " + e.getMessage();
            log.error(errorMessage, e);
            status = LlamaServerStatus.ERROR;
        }
    }

    /**
     * Stops the running llama-server process.
     */
    public synchronized void stop() {
        if (process == null) {
            status = LlamaServerStatus.STOPPED;
            return;
        }

        log.info("Stopping llama-server process");
        destroyProcess();
        status = LlamaServerStatus.STOPPED;
        activeModelPath = null;
        errorMessage = null;
        log.info("llama-server stopped");
    }

    /**
     * Restarts the llama-server process (stop → delay → start).
     */
    public void restart() {
        status = LlamaServerStatus.RESTARTING;
        stop();
        try {
            TimeUnit.SECONDS.sleep(properties.getRestartDelaySeconds());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        start();
    }

    /**
     * Switches the active model and restarts the llama-server.
     *
     * @param filename the GGUF model filename to switch to
     * @return the updated server status
     * @throws IllegalArgumentException if the model file is not found
     */
    public LlamaServerStatusDto switchModel(String filename) {
        Path modelPath = resolveModelPath(filename);
        if (modelPath == null) {
            throw new IllegalArgumentException("Model file not found in models directory: " + filename);
        }

        systemConfigService.setActiveModelFilename(filename);
        restart();
        return getStatus();
    }

    /**
     * Returns the current server status as a DTO.
     *
     * @return the current llama-server status
     */
    public LlamaServerStatusDto getStatus() {
        List<String> recentLines;
        synchronized (logBuffer) {
            recentLines = new ArrayList<>(logBuffer);
        }
        String activeModel = activeModelPath != null
                ? Path.of(activeModelPath).getFileName().toString()
                : null;

        return new LlamaServerStatusDto(
                status,
                activeModel,
                properties.getPort(),
                recentLines,
                errorMessage
        );
    }

    /**
     * Returns the most recent log lines from the llama-server process.
     *
     * @param n the maximum number of lines to return
     * @return a list of recent log lines
     */
    public List<String> getRecentLogLines(int n) {
        synchronized (logBuffer) {
            List<String> lines = new ArrayList<>(logBuffer);
            if (lines.size() > n) {
                return lines.subList(lines.size() - n, lines.size());
            }
            return lines;
        }
    }

    /**
     * Returns the absolute path to the currently loaded model file.
     *
     * @return the active model path, or null if not running
     */
    public String getActiveModelPath() {
        return activeModelPath;
    }

    /**
     * Scheduled health monitor. If the process was running but has exited,
     * triggers an automatic restart.
     */
    @Scheduled(fixedDelayString = "#{${app.inference.health-check-interval-seconds:30} * 1000}")
    public void monitorHealth() {
        if (status != LlamaServerStatus.RUNNING) {
            return;
        }

        if (process == null || !process.isAlive()) {
            log.warn("llama-server process died unexpectedly — restarting");
            process = null;
            restart();
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Private helpers
    // ──────────────────────────────────────────────────────────────

    private String resolveActiveModelFilename() {
        String fromConfig = systemConfigService.getActiveModelFilename();
        if (fromConfig != null && !fromConfig.isBlank()) {
            return fromConfig;
        }
        return properties.getActiveModel();
    }

    private Path resolveModelPath(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }

        Path modelsDir = Path.of(properties.getModelsDir());
        if (!Files.exists(modelsDir)) {
            return null;
        }

        // Search recursively up to 3 levels deep
        try (var stream = Files.walk(modelsDir, 3)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(filename))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            log.error("Failed to search models directory: {}", e.getMessage());
            return null;
        }
    }

    private void startLogCapture(Process proc) {
        Thread logThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (logBuffer) {
                        logBuffer.addLast(line);
                        while (logBuffer.size() > AppConstants.LLAMA_SERVER_LOG_BUFFER_LINES) {
                            logBuffer.removeFirst();
                        }
                    }
                    log.trace("[llama-server] {}", line);
                }
            } catch (IOException e) {
                log.debug("llama-server log stream ended: {}", e.getMessage());
            }
        }, "llama-server-log-capture");
        logThread.setDaemon(true);
        logThread.start();
    }

    private boolean waitForHealthy() {
        long deadline = System.currentTimeMillis()
                + (long) properties.getStartupTimeoutSeconds() * 1000;

        while (System.currentTimeMillis() < deadline) {
            if (process != null && !process.isAlive()) {
                log.error("llama-server process exited prematurely with code {}", process.exitValue());
                return false;
            }
            if (inferenceService.isAvailable()) {
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
