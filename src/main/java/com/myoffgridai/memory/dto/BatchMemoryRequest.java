package com.myoffgridai.memory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request body for batch memory operations (e.g. bulk delete).
 *
 * @param ids the list of memory IDs to operate on
 */
public record BatchMemoryRequest(
        @NotNull @Size(min = 1) List<UUID> ids
) {}
