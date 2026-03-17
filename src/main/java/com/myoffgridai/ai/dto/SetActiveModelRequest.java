package com.myoffgridai.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for setting the active model on the llama-server.
 *
 * @param filename the GGUF filename to load (must exist in the models directory)
 */
public record SetActiveModelRequest(
        @NotBlank(message = "filename is required")
        String filename
) {}
