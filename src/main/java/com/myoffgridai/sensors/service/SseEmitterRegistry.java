package com.myoffgridai.sensors.service;

import com.myoffgridai.config.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages SSE (Server-Sent Events) connections for live sensor streaming.
 * Each sensor ID maps to a list of connected emitters.
 */
@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> registry =
            new ConcurrentHashMap<>();

    /**
     * Registers a new SSE emitter for a sensor.
     *
     * @param sensorId the sensor ID
     * @param emitter  the SSE emitter
     */
    public void register(UUID sensorId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters =
                registry.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE emitter completed for sensor {}", sensorId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("SSE emitter timed out for sensor {}", sensorId);
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            log.debug("SSE emitter error for sensor {}: {}", sensorId, e.getMessage());
        });

        log.debug("Registered SSE emitter for sensor {}, total: {}", sensorId, emitters.size());
    }

    /**
     * Broadcasts a sensor reading to all connected emitters for the given sensor.
     *
     * @param sensorId   the sensor ID
     * @param value      the reading value
     * @param recordedAt the timestamp of the reading
     */
    public void broadcast(UUID sensorId, double value, Instant recordedAt) {
        CopyOnWriteArrayList<SseEmitter> emitters = registry.get(sensorId);
        if (emitters == null || emitters.isEmpty()) return;

        String data = String.format(
                "{\"value\":%.4f,\"recordedAt\":\"%s\",\"sensorId\":\"%s\"}",
                value, recordedAt.toString(), sensorId);

        List<SseEmitter> failed = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (IOException e) {
                log.debug("Failed to send SSE event to client for sensor {}", sensorId);
                failed.add(emitter);
            }
        }
        emitters.removeAll(failed);
    }

    /**
     * Completes and removes all emitters for a sensor.
     *
     * @param sensorId the sensor ID
     */
    public void remove(UUID sensorId) {
        CopyOnWriteArrayList<SseEmitter> emitters = registry.remove(sensorId);
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    // already completed or errored
                }
            }
            log.debug("Removed {} SSE emitters for sensor {}", emitters.size(), sensorId);
        }
    }
}
