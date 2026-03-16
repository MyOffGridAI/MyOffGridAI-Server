package com.myoffgridai.notification.controller;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.notification.dto.DeviceRegistrationDto;
import com.myoffgridai.notification.dto.RegisterDeviceRequest;
import com.myoffgridai.notification.service.DeviceRegistrationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing device registrations for MQTT push notifications.
 *
 * <p>All endpoints require authentication. Users can only manage their own devices.</p>
 */
@RestController
@RequestMapping("/api/notifications/devices")
public class DeviceRegistrationController {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistrationController.class);

    private final DeviceRegistrationService deviceRegistrationService;

    /**
     * Constructs the device registration controller.
     *
     * @param deviceRegistrationService the device registration service
     */
    public DeviceRegistrationController(DeviceRegistrationService deviceRegistrationService) {
        this.deviceRegistrationService = deviceRegistrationService;
    }

    /**
     * Registers or updates a device for MQTT push notifications.
     *
     * @param principal the authenticated user
     * @param request   the device registration request
     * @return the registered device DTO
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DeviceRegistrationDto>> registerDevice(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody RegisterDeviceRequest request) {
        log.info("User {} registering device {}", principal.getId(), request.deviceId());
        DeviceRegistrationDto dto = deviceRegistrationService.registerDevice(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(dto, "Device registered"));
    }

    /**
     * Lists all registered devices for the authenticated user.
     *
     * @param principal the authenticated user
     * @return list of device registration DTOs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceRegistrationDto>>> getDevices(
            @AuthenticationPrincipal User principal) {
        List<DeviceRegistrationDto> devices = deviceRegistrationService.getDevicesForUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    /**
     * Unregisters a device, removing it from MQTT notification delivery.
     *
     * @param principal the authenticated user
     * @param deviceId  the device identifier to remove
     * @return success response
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> unregisterDevice(
            @AuthenticationPrincipal User principal,
            @PathVariable String deviceId) {
        log.info("User {} unregistering device {}", principal.getId(), deviceId);
        deviceRegistrationService.unregisterDevice(principal.getId(), deviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Device unregistered"));
    }
}
