package com.myoffgridai.ai.judge;

import com.myoffgridai.config.InferenceProperties;
import com.myoffgridai.ai.service.ProcessBuilderFactory;
import com.myoffgridai.settings.dto.ExternalApiSettingsDto;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages a second llama-server process dedicated to the AI judge model.
 *
 * <p>Runs on a separate port ({@link JudgeProperties#getPort()}) from the
 * primary inference engine. Unlike the primary native inference service,
 * this service does not auto-start on boot or auto-restart on crash — if the
 * judge process dies, judge evaluation is simply skipped gracefully.</p>
 *
 * <p>Implements {@link DisposableBean} to terminate the process on shutdown.</p>
 */
@Service
public class JudgeModelProcessService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(JudgeModelProcessService.class);

    private final JudgeProperties judgeProperties;
    private final InferenceProperties llamaProperties;
    private final ProcessBuilderFactory processBuilderFactory;
    private final ExternalApiSettingsService externalApiSettingsService;

    private volatile Process process;

    /**
     * Constructs the judge model process service.
     *
     * @param judgeProperties            judge-specific configuration (port, context size, timeout)
     * @param llamaProperties            inference configuration (reuses binary path and models dir)
     * @param processBuilderFactory      factory for creating ProcessBuilder instances
     * @param externalApiSettingsService  DB-backed settings for enabled flag and model filename
     */
    public JudgeModelProcessService(JudgeProperties judgeProperties,
                                     InferenceProperties llamaProperties,
                                     ProcessBuilderFactory processBuilderFactory,
                                     ExternalApiSettingsService externalApiSettingsService) {
        this.judgeProperties = judgeProperties;
        this.llamaProperties = llamaProperties;
        this.processBuilderFactory = processBuilderFactory;
        this.externalApiSettingsService = externalApiSettingsService;
    }

    /**
     * Starts the judge llama-server process if not already running.
     *
     * <p>No-op if the judge is disabled, the process is already running,
     * or the configured model file does not exist.</p>
     */
    public synchronized void start() {
        ExternalApiSettingsDto settings = externalApiSettingsService.getSettings();

        if (!settings.judgeEnabled()) {
            log.info("Judge model is disabled — skipping start");
            return;
        }

        if (process != null && process.isAlive()) {
            log.info("Judge process already running — ignoring start request");
            return;
        }

        String modelFilename = settings.judgeModelFilename();
        if (modelFilename == null || modelFilename.isBlank()) {
            log.warn("No judge model filename configured — cannot start judge process");
            return;
        }

        Path modelPath = resolveModelPath(modelFilename);
        if (modelPath == null) {
            log.warn("Judge model file not found: {} — cannot start judge process", modelFilename);
            return;
        }

        String binary = llamaProperties.getLlamaServerBinary();
        if (binary == null || !Files.exists(Path.of(binary))) {
            log.warn("llama-server binary not found: {} — cannot start judge process", binary);
            return;
        }

        List<String> command = List.of(
                binary,
                "-m", modelPath.toString(),
                "--port", String.valueOf(judgeProperties.getPort()),
                "-c", String.valueOf(judgeProperties.getContextSize()),
                "--host", "127.0.0.1"
        );

        try {
            ProcessBuilder pb = processBuilderFactory.create(command);
            pb.redirectErrorStream(true);
            process = pb.start();
            log.info("Judge llama-server started with PID {} — model: {}", process.pid(), modelPath);

            if (waitForHealthy()) {
                log.info("Judge llama-server is healthy on port {}", judgeProperties.getPort());
            } else {
                log.warn("Judge llama-server did not become healthy within timeout — stopping");
                destroyProcess();
            }

        } catch (IOException e) {
            log.error("Failed to start judge llama-server: {}", e.getMessage(), e);
        }
    }

    /**
     * Stops the judge llama-server process if running.
     */
    public synchronized void stop() {
        if (process == null) {
            return;
        }
        log.info("Stopping judge llama-server process");
        destroyProcess();
        log.info("Judge llama-server stopped");
    }

    /**
     * Returns whether the judge process is alive and responding to health checks.
     *
     * @return true if the process is alive and the health endpoint returns 200
     */
    public boolean isRunning() {
        return process != null && process.isAlive() && checkHealth();
    }

    /**
     * Returns the HTTP port the judge process listens on.
     *
     * @return the judge port
     */
    public int getPort() {
        return judgeProperties.getPort();
    }

    /**
     * Terminates the judge process on application shutdown.
     */
    @Override
    public void destroy() {
        stop();
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private Path resolveModelPath(String filename) {
        Path modelsDir = Path.of(llamaProperties.getModelsDir());
        if (!Files.exists(modelsDir)) {
            return null;
        }
        try (var stream = Files.walk(modelsDir, 3)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(filename))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            log.error("Failed to search models directory for judge model: {}", e.getMessage());
            return null;
        }
    }

    private boolean waitForHealthy() {
        long deadline = System.currentTimeMillis()
                + (long) judgeProperties.getTimeoutSeconds() * 1000;

        while (System.currentTimeMillis() < deadline) {
            if (process != null && !process.isAlive()) {
                log.error("Judge llama-server exited prematurely with code {}", process.exitValue());
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
                    "http://localhost:" + judgeProperties.getPort() + "/health"
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
