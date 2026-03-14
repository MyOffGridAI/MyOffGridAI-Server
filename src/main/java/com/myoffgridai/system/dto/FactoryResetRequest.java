package com.myoffgridai.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for performing a factory reset. Requires a confirmation phrase
 * to prevent accidental invocations.
 *
 * @param confirmPhrase must equal "RESET MY DEVICE" to proceed
 */
public record FactoryResetRequest(
        @NotBlank(message = "Confirmation phrase is required")
        String confirmPhrase
) {
}
