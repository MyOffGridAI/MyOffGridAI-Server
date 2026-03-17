package com.myoffgridai.config;

import com.myoffgridai.ai.service.ProcessBuilderFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ProcessConfig} bean creation.
 */
class ProcessConfigTest {

    @Test
    void processBuilderFactory_createsProcessBuilder() {
        ProcessConfig config = new ProcessConfig();
        ProcessBuilderFactory factory = config.processBuilderFactory();

        assertNotNull(factory);

        // Verify it creates a real ProcessBuilder
        List<String> command = List.of("echo", "hello");
        ProcessBuilder pb = factory.create(command);

        assertNotNull(pb);
        assertEquals(command, pb.command());
    }
}
