package com.myoffgridai.memory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request body for batch-updating the shared visibility of memories.
 *
 * @param ids    the list of memory IDs to update
 * @param shared the new shared visibility value
 */
public record BatchUpdateSharedRequest(
        @NotNull @Size(min = 1) List<UUID> ids,
        @NotNull Boolean shared
) {}
