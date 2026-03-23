package com.myoffgridai.memory.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for updating the shared visibility of a memory.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record UpdateSharedRequest(
        @NotNull Boolean shared
) {}
