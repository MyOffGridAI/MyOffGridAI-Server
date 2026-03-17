package com.myoffgridai.ai.judge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JudgeProperties}.
 *
 * <p>Validates default values and setter/getter round-trips for all
 * judge configuration properties.</p>
 */
class JudgePropertiesTest {

    @Test
    void defaults_matchExpectedValues() {
        JudgeProperties props = new JudgeProperties();

        assertFalse(props.isEnabled());
        assertEquals("", props.getModelFilename());
        assertEquals(1235, props.getPort());
        assertEquals(7.5, props.getScoreThreshold());
        assertEquals(30, props.getTimeoutSeconds());
        assertEquals(4096, props.getContextSize());
    }

    @Test
    void setEnabled_roundTrips() {
        JudgeProperties props = new JudgeProperties();
        props.setEnabled(true);
        assertTrue(props.isEnabled());
    }

    @Test
    void setModelFilename_roundTrips() {
        JudgeProperties props = new JudgeProperties();
        props.setModelFilename("judge-model.gguf");
        assertEquals("judge-model.gguf", props.getModelFilename());
    }

    @Test
    void setPort_roundTrips() {
        JudgeProperties props = new JudgeProperties();
        props.setPort(9999);
        assertEquals(9999, props.getPort());
    }

    @Test
    void setScoreThreshold_roundTrips() {
        JudgeProperties props = new JudgeProperties();
        props.setScoreThreshold(5.0);
        assertEquals(5.0, props.getScoreThreshold());
    }

    @Test
    void setTimeoutSeconds_roundTrips() {
        JudgeProperties props = new JudgeProperties();
        props.setTimeoutSeconds(60);
        assertEquals(60, props.getTimeoutSeconds());
    }

    @Test
    void setContextSize_roundTrips() {
        JudgeProperties props = new JudgeProperties();
        props.setContextSize(8192);
        assertEquals(8192, props.getContextSize());
    }
}
