package com.myoffgridai.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for connecting to a WiFi network during setup.
 *
 * @param ssid     the WiFi network name to connect to
 * @param password the WiFi network password
 */
public record WifiConnectRequest(
        @NotBlank(message = "SSID is required")
        String ssid,
        String password
) {
}
