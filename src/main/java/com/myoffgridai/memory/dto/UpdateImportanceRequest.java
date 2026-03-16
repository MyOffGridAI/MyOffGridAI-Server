package com.myoffgridai.memory.dto;

import com.myoffgridai.memory.model.MemoryImportance;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for updating the importance level of a memory.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record UpdateImportanceRequest(
        @NotNull MemoryImportance importance
) {}
