package com.myoffgridai.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LlamaServerProperties} default values and setters.
 */
class LlamaServerPropertiesTest {

    @Test
    void defaults_areSetCorrectly() {
        LlamaServerProperties props = new LlamaServerProperties();

        assertEquals("/opt/homebrew/bin/llama-server", props.getBinary());
        assertEquals("./models", props.getModelsDir());
        assertEquals("", props.getActiveModel());
        assertEquals(1234, props.getPort());
        assertEquals(32768, props.getContextSize());
        assertEquals(99, props.getGpuLayers());
        assertEquals(8, props.getThreads());
        assertEquals(120, props.getStartupTimeoutSeconds());
        assertEquals(30, props.getHealthCheckIntervalSeconds());
    }

    @Test
    void setters_updateValues() {
        LlamaServerProperties props = new LlamaServerProperties();

        props.setBinary("/usr/local/bin/llama-server");
        props.setModelsDir("/data/models");
        props.setActiveModel("test-model.gguf");
        props.setPort(5678);
        props.setContextSize(8192);
        props.setGpuLayers(32);
        props.setThreads(4);
        props.setStartupTimeoutSeconds(60);
        props.setHealthCheckIntervalSeconds(15);

        assertEquals("/usr/local/bin/llama-server", props.getBinary());
        assertEquals("/data/models", props.getModelsDir());
        assertEquals("test-model.gguf", props.getActiveModel());
        assertEquals(5678, props.getPort());
        assertEquals(8192, props.getContextSize());
        assertEquals(32, props.getGpuLayers());
        assertEquals(4, props.getThreads());
        assertEquals(60, props.getStartupTimeoutSeconds());
        assertEquals(15, props.getHealthCheckIntervalSeconds());
    }
}
