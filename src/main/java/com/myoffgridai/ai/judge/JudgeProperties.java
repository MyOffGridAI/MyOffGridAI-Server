package com.myoffgridai.ai.judge;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized configuration for the AI judge model.
 *
 * <p>Bound from the {@code app.judge} prefix in {@code application.yml}.
 * Controls whether the judge pipeline is active, which model file to load,
 * and the scoring threshold that triggers cloud refinement.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "app.judge")
public class JudgeProperties {

    /** Whether the judge evaluation pipeline is enabled. */
    private boolean enabled = false;

    /** Filename of the judge GGUF model (relative to the models directory). */
    private String modelFilename = "";

    /** HTTP port for the judge llama-server instance. */
    private int port = 1235;

    /** Minimum score (1–10) below which the judge recommends cloud refinement. */
    private double scoreThreshold = 7.5;

    /** Timeout in seconds for judge inference and health check polling. */
    private int timeoutSeconds = 30;

    /** Context window size in tokens for the judge model. */
    private int contextSize = 4096;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getModelFilename() {
        return modelFilename;
    }

    public void setModelFilename(String modelFilename) {
        this.modelFilename = modelFilename;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public double getScoreThreshold() {
        return scoreThreshold;
    }

    public void setScoreThreshold(double scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getContextSize() {
        return contextSize;
    }

    public void setContextSize(int contextSize) {
        this.contextSize = contextSize;
    }
}
