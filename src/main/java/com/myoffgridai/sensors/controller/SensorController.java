package com.myoffgridai.sensors.controller;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.sensors.dto.*;
import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.model.SensorReading;
import com.myoffgridai.sensors.service.SensorService;
import com.myoffgridai.sensors.service.SseEmitterRegistry;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for sensor management, reading history, connection testing,
 * and live SSE streaming.
 */
@RestController
@RequestMapping(AppConstants.SENSORS_API_PATH)
public class SensorController {

    private static final Logger log = LoggerFactory.getLogger(SensorController.class);

    private final SensorService sensorService;
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * Constructs the sensor controller.
     *
     * @param sensorService      the sensor service
     * @param sseEmitterRegistry the SSE emitter registry
     */
    public SensorController(SensorService sensorService,
                            SseEmitterRegistry sseEmitterRegistry) {
        this.sensorService = sensorService;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    // ── Sensor CRUD ─────────────────────────────────────────────────────────

    /**
     * Lists all sensors for the authenticated user.
     *
     * @param principal the authenticated user
     * @return list of sensor DTOs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SensorDto>>> listSensors(
            @AuthenticationPrincipal User principal) {
        List<Sensor> sensors = sensorService.listSensors(principal.getId());
        List<SensorDto> dtos = sensors.stream().map(SensorDto::from).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * Gets a single sensor by ID.
     *
     * @param principal the authenticated user
     * @param sensorId  the sensor ID
     * @return the sensor DTO
     */
    @GetMapping("/{sensorId}")
    public ResponseEntity<ApiResponse<SensorDto>> getSensor(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID sensorId) {
        Sensor sensor = sensorService.getSensor(sensorId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(SensorDto.from(sensor)));
    }

    /**
     * Registers a new sensor.
     *
     * @param principal the authenticated user
     * @param request   the creation request
     * @return the created sensor DTO
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SensorDto>> registerSensor(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody CreateSensorRequest request) {
        Sensor sensor = sensorService.registerSensor(principal.getId(), request);
        log.info("User {} registered sensor '{}'", principal.getId(), sensor.getName());
        return ResponseEntity.ok(ApiResponse.success(SensorDto.from(sensor), "Sensor registered"));
    }

    /**
     * Deletes a sensor and all its readings.
     *
     * @param principal the authenticated user
     * @param sensorId  the sensor ID
     * @return success response
     */
    @DeleteMapping("/{sensorId}")
    public ResponseEntity<ApiResponse<Void>> deleteSensor(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID sensorId) {
        sensorService.deleteSensor(sensorId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Sensor deleted"));
    }

    // ── Start / Stop ────────────────────────────────────────────────────────

    /**
     * Starts polling for a sensor.
     *
     * @param principal the authenticated user
     * @param sensorId  the sensor ID
     * @return the updated sensor DTO
     */
    @PostMapping("/{sensorId}/start")
    public ResponseEntity<ApiResponse<SensorDto>> startSensor(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID sensorId) {
        Sensor sensor = sensorService.startSensor(sensorId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(SensorDto.from(sensor), "Sensor started"));
    }

    /**
     * Stops polling for a sensor.
     *
     * @param principal the authenticated user
     * @param sensorId  the sensor ID
     * @return the updated sensor DTO
     */
    @PostMapping("/{sensorId}/stop")
    public ResponseEntity<ApiResponse<SensorDto>> stopSensor(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID sensorId) {
        Sensor sensor = sensorService.stopSensor(sensorId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(SensorDto.from(sensor), "Sensor stopped"));
    }

    // ── Readings ────────────────────────────────────────────────────────────

    /**
     * Gets the latest reading for a sensor.
     *
     * @param principal the authenticated user
     * @param sensorId  the sensor ID
     * @return the latest reading DTO, or null if none
     */
    @GetMapping("/{sensorId}/latest")
    public ResponseEntity<ApiResponse<SensorReadingDto>> getLatestReading(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID sensorId) {
        Optional<SensorReading> reading = sensorService.getLatestReading(sensorId, principal.getId());
        SensorReadingDto dto = reading.map(SensorReadingDto::from).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * Gets paginated reading history for a sensor.
     *
     * @param principal the authenticated user
     * @param sensorId  the sensor ID
     * @param hours     number of hours of history (max 168)
     * @param page      the page number (0-based)
     * @param size      the page size
     * @return paginated list of reading DTOs
     */
    @GetMapping("/{sensorId}/history")
    public ResponseEntity<ApiResponse<List<SensorReadingDto>>> getReadingHistory(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID sensorId,
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        hours = Math.min(hours, AppConstants.SENSOR_READING_HISTORY_MAX_HOURS);
        Page<SensorReading> readings = sensorService.getReadingHistory(
                sensorId, principal.getId(), hours, page, size);
        List<SensorReadingDto> dtos = readings.getContent().stream()
                .map(SensorReadingDto::from).toList();
        return ResponseEntity.ok(ApiResponse.paginated(
                dtos, readings.getTotalElements(), page, size));
    }

    // ── Thresholds ──────────────────────────────────────────────────────────

    /**
     * Updates threshold values for a sensor.
     *
     * @param principal the authenticated user
     * @param sensorId  the sensor ID
     * @param request   the threshold update request
     * @return the updated sensor DTO
     */
    @PutMapping("/{sensorId}/thresholds")
    public ResponseEntity<ApiResponse<SensorDto>> updateThresholds(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID sensorId,
            @Valid @RequestBody UpdateThresholdsRequest request) {
        Sensor sensor = sensorService.updateThresholds(sensorId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(SensorDto.from(sensor), "Thresholds updated"));
    }

    // ── Connection Testing ──────────────────────────────────────────────────

    /**
     * Tests a serial port connection and returns sample data if available.
     *
     * @param request the test request with port path and baud rate
     * @return the test result
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<SensorTestResult>> testConnection(
            @Valid @RequestBody TestSensorRequest request) {
        int baudRate = request.baudRate() != null ? request.baudRate() : 9600;
        SensorTestResult result = sensorService.testSensor(request.portPath(), baudRate);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Lists all available serial ports on the device.
     *
     * @return list of port path strings
     */
    @GetMapping("/ports")
    public ResponseEntity<ApiResponse<List<String>>> listAvailablePorts() {
        List<String> ports = sensorService.listAvailablePorts();
        return ResponseEntity.ok(ApiResponse.success(ports));
    }

    // ── SSE Streaming ───────────────────────────────────────────────────────

    /**
     * Opens a Server-Sent Events stream for live sensor readings.
     *
     * @param principal the authenticated user
     * @param sensorId  the sensor ID to stream
     * @return an SSE emitter that broadcasts readings in real-time
     */
    @GetMapping(value = "/{sensorId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSensor(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID sensorId) {
        sensorService.getSensor(sensorId, principal.getId());
        SseEmitter emitter = new SseEmitter(AppConstants.SSE_EMITTER_TIMEOUT_MS);
        sseEmitterRegistry.register(sensorId, emitter);
        log.info("User {} opened SSE stream for sensor {}", principal.getId(), sensorId);
        return emitter;
    }
}
