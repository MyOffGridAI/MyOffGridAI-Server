package com.myoffgridai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties specific to the native java-llama.cpp inference provider.
 *
 * <p>Binds properties under the {@code app.inference.native} prefix.
 * These settings control the JNI-based in-process model loading and inference
 * parameters that are unique to the native provider.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.inference.native")
public class NativeLlamaProperties {

    private int contextSize = 32768;
    private int gpuLayers = 99;
    private int threads = 8;
    private boolean enableEmbedding = false;
    private int embeddingContextSize = 2048;

    /** @return the context window size in tokens for the native model */
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

    /** @return whether native embedding is enabled (false = delegate to Ollama) */
    public boolean isEnableEmbedding() {
        return enableEmbedding;
    }

    public void setEnableEmbedding(boolean enableEmbedding) {
        this.enableEmbedding = enableEmbedding;
    }

    /** @return context size for embedding model when native embedding is enabled */
    public int getEmbeddingContextSize() {
        return embeddingContextSize;
    }

    public void setEmbeddingContextSize(int embeddingContextSize) {
        this.embeddingContextSize = embeddingContextSize;
    }
}
