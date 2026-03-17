package com.myoffgridai.system.dto;

import java.time.Instant;

/**
 * Data transfer object representing the current system initialization and configuration status.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record SystemStatusDto(
        boolean initialized,
        String instanceName,
        boolean fortressEnabled,
        boolean wifiConfigured,
        String serverVersion,
        Instant timestamp,
        String inferenceProvider,
        String inferenceProviderStatus,
        String activeModelFilename
) {}
