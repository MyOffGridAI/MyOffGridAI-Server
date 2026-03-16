package com.myoffgridai.notification.dto;

import com.myoffgridai.notification.model.DeviceRegistration;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a registered client device.
 *
 * @param id           the registration UUID
 * @param deviceId     the unique device identifier
 * @param deviceName   the human-readable device label
 * @param platform     the device platform
 * @param mqttClientId the MQTT client ID
 * @param lastSeenAt   when the device was last seen
 */
public record DeviceRegistrationDto(
        UUID id,
        String deviceId,
        String deviceName,
        String platform,
        String mqttClientId,
        Instant lastSeenAt
) {

    /**
     * Creates a DTO from a {@link DeviceRegistration} entity.
     *
     * @param entity the device registration entity
     * @return the corresponding DTO
     */
    public static DeviceRegistrationDto from(DeviceRegistration entity) {
        return new DeviceRegistrationDto(
                entity.getId(),
                entity.getDeviceId(),
                entity.getDeviceName(),
                entity.getPlatform(),
                entity.getMqttClientId(),
                entity.getLastSeenAt()
        );
    }
}
