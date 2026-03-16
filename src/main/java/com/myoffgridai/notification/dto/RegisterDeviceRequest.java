package com.myoffgridai.notification.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for registering or updating a client device for MQTT push notifications.
 *
 * @param deviceId     the unique device identifier from the Flutter client
 * @param deviceName   a human-readable label (e.g., "Adam's Phone")
 * @param platform     the device platform ("android", "ios", "web")
 * @param mqttClientId the MQTT client ID the device uses to subscribe
 */
public record RegisterDeviceRequest(
        @NotBlank String deviceId,
        String deviceName,
        @NotBlank String platform,
        @NotBlank String mqttClientId
) {
}
