package com.myoffgridai.ai.dto;

import com.myoffgridai.ai.service.NativeLlamaStatus;

/**
 * Data transfer object representing the current state of the native inference engine.
 *
 * @param status         the current engine lifecycle status
 * @param activeModel    the filename of the currently loaded model, or null
 * @param errorMessage   a descriptive error message if status is ERROR, or null
 * @param memoryUsageMb  estimated memory usage in megabytes, or null if unknown
 */
public record NativeLlamaStatusDto(
        NativeLlamaStatus status,
        String activeModel,
        String errorMessage,
        Long memoryUsageMb
) {}
