package com.myoffgridai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for inference providers and shared model management.
 *
 * <p>Binds properties under the {@code app.inference} prefix including
 * the binary path, models directory, active model, server port, context
 * size, GPU layers, thread count, and health/restart settings.</p>
 *
 * <p>Used by both the primary inference provider and by
 * {@link com.myoffgridai.ai.judge.JudgeModelProcessService} for the judge
 * llama-server process (which needs the binary path and models directory).</p>
 */
@Component
@ConfigurationProperties(prefix = "app.inference")
public class InferenceProperties {

    private String llamaServerBinary;
    private String modelsDir;
    private String activeModel;
    private int port = 1234;
    private int contextSize = 32768;
    private int gpuLayers = 99;
    private int threads = 8;
    private int healthCheckIntervalSeconds = 30;
    private int restartDelaySeconds = 5;
    private int startupTimeoutSeconds = 120;

    /** @return absolute path to the llama-server binary */
    public String getLlamaServerBinary() {
        return llamaServerBinary;
    }

    public void setLlamaServerBinary(String llamaServerBinary) {
        this.llamaServerBinary = llamaServerBinary;
    }

    /** @return directory where GGUF model files are stored */
    public String getModelsDir() {
        return modelsDir;
    }

    public void setModelsDir(String modelsDir) {
        this.modelsDir = modelsDir;
    }

    /** @return the filename of the active model to load at startup */
    public String getActiveModel() {
        return activeModel;
    }

    public void setActiveModel(String activeModel) {
        this.activeModel = activeModel;
    }

    /** @return the HTTP port for llama-server to listen on */
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /** @return the context window size in tokens */
    public int getContextSize() {
        return contextSize;
    }

    public void setContextSize(int contextSize) {
        this.contextSize = contextSize;
    }

    /** @return the number of layers to offload to GPU */
    public int getGpuLayers() {
        return gpuLayers;
    }

    public void setGpuLayers(int gpuLayers) {
        this.gpuLayers = gpuLayers;
    }

    /** @return the number of CPU threads for inference */
    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    /** @return interval in seconds between health checks for crash detection */
    public int getHealthCheckIntervalSeconds() {
        return healthCheckIntervalSeconds;
    }

    public void setHealthCheckIntervalSeconds(int healthCheckIntervalSeconds) {
        this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
    }

    /** @return delay in seconds before restarting after a crash */
    public int getRestartDelaySeconds() {
        return restartDelaySeconds;
    }

    public void setRestartDelaySeconds(int restartDelaySeconds) {
        this.restartDelaySeconds = restartDelaySeconds;
    }

    /** @return timeout in seconds for llama-server to become healthy after start */
    public int getStartupTimeoutSeconds() {
        return startupTimeoutSeconds;
    }

    public void setStartupTimeoutSeconds(int startupTimeoutSeconds) {
        this.startupTimeoutSeconds = startupTimeoutSeconds;
    }
}
