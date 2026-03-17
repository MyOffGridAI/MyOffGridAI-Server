package com.myoffgridai.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NativeLlamaProperties} default values and setters.
 */
class NativeLlamaPropertiesTest {

    @Test
    void defaults_areSetCorrectly() {
        NativeLlamaProperties props = new NativeLlamaProperties();

        assertEquals(32768, props.getContextSize());
        assertEquals(99, props.getGpuLayers());
        assertEquals(8, props.getThreads());
        assertFalse(props.isEnableEmbedding());
        assertEquals(2048, props.getEmbeddingContextSize());
    }

    @Test
    void setters_updateValues() {
        NativeLlamaProperties props = new NativeLlamaProperties();

        props.setContextSize(4096);
        props.setGpuLayers(32);
        props.setThreads(4);
        props.setEnableEmbedding(true);
        props.setEmbeddingContextSize(1024);

        assertEquals(4096, props.getContextSize());
        assertEquals(32, props.getGpuLayers());
        assertEquals(4, props.getThreads());
        assertTrue(props.isEnableEmbedding());
        assertEquals(1024, props.getEmbeddingContextSize());
    }
}
