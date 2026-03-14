package com.myoffgridai.memory.dto;

import com.myoffgridai.memory.model.MemoryImportance;
import jakarta.validation.constraints.NotNull;

public record UpdateImportanceRequest(
        @NotNull MemoryImportance importance
) {}
