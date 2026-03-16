package com.myoffgridai.notification.service;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.notification.dto.DeviceRegistrationDto;
import com.myoffgridai.notification.dto.RegisterDeviceRequest;
import com.myoffgridai.notification.model.DeviceRegistration;
import com.myoffgridai.notification.repository.DeviceRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Manages device registrations for MQTT push notification delivery.
 *
 * <p>Devices are registered by the Flutter client at startup or login.
 * The server uses these registrations to determine which MQTT topics
 * to publish notifications to for a given user.</p>
 */
@Service
public class DeviceRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistrationService.class);

    private final DeviceRegistrationRepository repository;

    /**
     * Constructs the device registration service.
     *
     * @param repository the device registration repository
     */
    public DeviceRegistrationService(DeviceRegistrationRepository repository) {
        this.repository = repository;
    }

    /**
     * Registers or updates a device for MQTT notifications.
     *
     * <p>If a registration with the same {@code (userId, deviceId)} already exists,
     * the existing record is updated with the new MQTT client ID, device name,
     * and last-seen timestamp. Otherwise, a new registration is created.</p>
     *
     * @param userId  the authenticated user ID
     * @param request the device registration request
     * @return the persisted device registration DTO
     */
    public DeviceRegistrationDto registerDevice(UUID userId, RegisterDeviceRequest request) {
        DeviceRegistration registration = repository.findByUserIdAndDeviceId(userId, request.deviceId())
                .map(existing -> {
                    existing.setMqttClientId(request.mqttClientId());
                    existing.setDeviceName(request.deviceName());
                    existing.setPlatform(request.platform());
                    existing.setLastSeenAt(Instant.now());
                    log.info("Updated device registration for user {} device {}", userId, request.deviceId());
                    return existing;
                })
                .orElseGet(() -> {
                    DeviceRegistration newReg = new DeviceRegistration();
                    newReg.setUserId(userId);
                    newReg.setDeviceId(request.deviceId());
                    newReg.setDeviceName(request.deviceName());
                    newReg.setPlatform(request.platform());
                    newReg.setMqttClientId(request.mqttClientId());
                    newReg.setLastSeenAt(Instant.now());
                    log.info("Created device registration for user {} device {}", userId, request.deviceId());
                    return newReg;
                });

        return DeviceRegistrationDto.from(repository.save(registration));
    }

    /**
     * Returns all registered devices for a user.
     *
     * @param userId the user ID
     * @return list of device registration DTOs
     */
    public List<DeviceRegistrationDto> getDevicesForUser(UUID userId) {
        return repository.findByUserId(userId).stream()
                .map(DeviceRegistrationDto::from)
                .toList();
    }

    /**
     * Unregisters a device, removing it from MQTT notification delivery.
     *
     * @param userId   the user ID
     * @param deviceId the device identifier to remove
     */
    @Transactional
    public void unregisterDevice(UUID userId, String deviceId) {
        repository.deleteByUserIdAndDeviceId(userId, deviceId);
        log.info("Unregistered device {} for user {}", deviceId, userId);
    }

    /**
     * Returns formatted MQTT topic strings for all devices belonging to a user.
     *
     * @param userId the user ID
     * @return list of MQTT topic strings
     */
    public List<String> getTopicsForUser(UUID userId) {
        return repository.findByUserId(userId).stream()
                .map(device -> String.format(AppConstants.MQTT_TOPIC_USER_NOTIFICATIONS, userId.toString()))
                .distinct()
                .toList();
    }
}
