package com.myoffgridai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized configuration for the llama-server HTTP inference provider.
 *
 * <p>Bound from the {@code app.llama-server} prefix in {@code application.yml}.
 * Controls the path to the Homebrew-installed llama-server binary, the models
 * directory, active model filename, server port, context size, GPU layer
 * offloading, CPU thread count, and health-check timing.</p>
 *
 * <p>On the shipped appliance the binary is pre-installed at a known path;
 * no runtime download is required.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "app.llama-server")
public class LlamaServerProperties {

    /** Path to the llama-server binary. */
    private String binary = "/opt/homebrew/bin/llama-server";

    /** Directory containing GGUF model files. Supports nested subdirectories. */
    private String modelsDir = "./models";

    /** Active model filename relative to modelsDir. May include subdirectory path. */
    private String activeModel = "";

    /** Port llama-server listens on. */
    private int port = 1234;

    /** Model context size in tokens. */
    private int contextSize = 32768;

    /**
     * Number of model layers to offload to GPU.
     * 99 = offload all layers (recommended for Apple Silicon with unified memory).
     */
    private int gpuLayers = 99;

    /** Number of CPU threads for inference. */
    private int threads = 8;

    /** Seconds to wait for llama-server to become healthy after launch. */
    private int startupTimeoutSeconds = 120;

    /** Seconds between health check polls. */
    private int healthCheckIntervalSeconds = 30;

    /** @return absolute path to the llama-server binary */
    public String getBinary() {
        return binary;
    }

    public void setBinary(String binary) {
        this.binary = binary;
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

    /** @return the number of layers to offload to GPU (99 = all) */
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

    /** @return timeout in seconds for llama-server to become healthy after start */
    public int getStartupTimeoutSeconds() {
        return startupTimeoutSeconds;
    }

    public void setStartupTimeoutSeconds(int startupTimeoutSeconds) {
        this.startupTimeoutSeconds = startupTimeoutSeconds;
    }

    /** @return interval in seconds between periodic health checks */
    public int getHealthCheckIntervalSeconds() {
        return healthCheckIntervalSeconds;
    }

    public void setHealthCheckIntervalSeconds(int healthCheckIntervalSeconds) {
        this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
    }
}
