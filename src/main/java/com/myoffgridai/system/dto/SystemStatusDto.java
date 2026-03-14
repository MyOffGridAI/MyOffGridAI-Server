package com.myoffgridai.system.dto;

import java.time.Instant;

public record SystemStatusDto(
        boolean initialized,
        String instanceName,
        boolean fortressEnabled,
        boolean wifiConfigured,
        String serverVersion,
        Instant timestamp
) {}
