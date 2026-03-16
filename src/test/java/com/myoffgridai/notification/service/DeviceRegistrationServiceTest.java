package com.myoffgridai.notification.service;

import com.myoffgridai.notification.dto.DeviceRegistrationDto;
import com.myoffgridai.notification.dto.RegisterDeviceRequest;
import com.myoffgridai.notification.model.DeviceRegistration;
import com.myoffgridai.notification.repository.DeviceRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeviceRegistrationService}.
 */
@ExtendWith(MockitoExtension.class)
class DeviceRegistrationServiceTest {

    @Mock
    private DeviceRegistrationRepository repository;

    private DeviceRegistrationService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new DeviceRegistrationService(repository);
        userId = UUID.randomUUID();
    }

    @Test
    void registerDevice_newDevice_createsRegistration() {
        RegisterDeviceRequest request = new RegisterDeviceRequest(
                "device-1", "Adam's Phone", "android", "mqtt-client-1");
        when(repository.findByUserIdAndDeviceId(userId, "device-1")).thenReturn(Optional.empty());
        when(repository.save(any(DeviceRegistration.class))).thenAnswer(i -> {
            DeviceRegistration reg = i.getArgument(0);
            reg.setId(UUID.randomUUID());
            return reg;
        });

        DeviceRegistrationDto result = service.registerDevice(userId, request);

        assertNotNull(result.id());
        assertEquals("device-1", result.deviceId());
        assertEquals("Adam's Phone", result.deviceName());
        assertEquals("android", result.platform());
        assertEquals("mqtt-client-1", result.mqttClientId());
    }

    @Test
    void registerDevice_existingDevice_updatesRegistration() {
        DeviceRegistration existing = createRegistration("device-1");
        RegisterDeviceRequest request = new RegisterDeviceRequest(
                "device-1", "New Name", "ios", "mqtt-client-2");
        when(repository.findByUserIdAndDeviceId(userId, "device-1")).thenReturn(Optional.of(existing));
        when(repository.save(any(DeviceRegistration.class))).thenAnswer(i -> i.getArgument(0));

        DeviceRegistrationDto result = service.registerDevice(userId, request);

        assertEquals("New Name", result.deviceName());
        assertEquals("ios", result.platform());
        assertEquals("mqtt-client-2", result.mqttClientId());
    }

    @Test
    void getDevicesForUser_returnsList() {
        DeviceRegistration reg = createRegistration("device-1");
        when(repository.findByUserId(userId)).thenReturn(List.of(reg));

        List<DeviceRegistrationDto> result = service.getDevicesForUser(userId);

        assertEquals(1, result.size());
        assertEquals("device-1", result.get(0).deviceId());
    }

    @Test
    void getDevicesForUser_emptyList() {
        when(repository.findByUserId(userId)).thenReturn(List.of());

        List<DeviceRegistrationDto> result = service.getDevicesForUser(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void unregisterDevice_callsDelete() {
        service.unregisterDevice(userId, "device-1");

        verify(repository).deleteByUserIdAndDeviceId(userId, "device-1");
    }

    @Test
    void getTopicsForUser_returnsFormattedTopics() {
        DeviceRegistration reg = createRegistration("device-1");
        when(repository.findByUserId(userId)).thenReturn(List.of(reg));

        List<String> topics = service.getTopicsForUser(userId);

        assertEquals(1, topics.size());
        assertTrue(topics.get(0).contains(userId.toString()));
    }

    @Test
    void getTopicsForUser_noDevices_returnsEmpty() {
        when(repository.findByUserId(userId)).thenReturn(List.of());

        List<String> topics = service.getTopicsForUser(userId);

        assertTrue(topics.isEmpty());
    }

    @Test
    void getTopicsForUser_multipleDevices_returnsDistinct() {
        DeviceRegistration reg1 = createRegistration("device-1");
        DeviceRegistration reg2 = createRegistration("device-2");
        when(repository.findByUserId(userId)).thenReturn(List.of(reg1, reg2));

        List<String> topics = service.getTopicsForUser(userId);

        // Same user = same topic, so should be deduplicated
        assertEquals(1, topics.size());
    }

    private DeviceRegistration createRegistration(String deviceId) {
        DeviceRegistration reg = new DeviceRegistration();
        reg.setId(UUID.randomUUID());
        reg.setUserId(userId);
        reg.setDeviceId(deviceId);
        reg.setDeviceName("Test Device");
        reg.setPlatform("android");
        reg.setMqttClientId("mqtt-" + deviceId);
        reg.setLastSeenAt(Instant.now());
        reg.setCreatedAt(Instant.now());
        return reg;
    }
}
