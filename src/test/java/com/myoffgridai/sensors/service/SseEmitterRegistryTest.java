package com.myoffgridai.sensors.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SseEmitterRegistryTest {

    private SseEmitterRegistry registry;
    private UUID sensorId;

    @BeforeEach
    void setUp() {
        registry = new SseEmitterRegistry();
        sensorId = UUID.randomUUID();
    }

    @Test
    void register_addsEmitter() {
        SseEmitter emitter = new SseEmitter();
        registry.register(sensorId, emitter);
        // No exception means success — we can verify via broadcast
        assertDoesNotThrow(() -> registry.broadcast(sensorId, 25.0, Instant.now()));
    }

    @Test
    void broadcast_sendsToRegisteredEmitters() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(sensorId, emitter);

        registry.broadcast(sensorId, 25.5, Instant.now());

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void broadcast_noEmitters_doesNotThrow() {
        assertDoesNotThrow(() -> registry.broadcast(UUID.randomUUID(), 25.0, Instant.now()));
    }

    @Test
    void broadcast_removesFailedEmitters() throws IOException {
        SseEmitter goodEmitter = mock(SseEmitter.class);
        SseEmitter badEmitter = mock(SseEmitter.class);
        doThrow(new IOException("broken")).when(badEmitter).send(any(SseEmitter.SseEventBuilder.class));

        registry.register(sensorId, goodEmitter);
        registry.register(sensorId, badEmitter);

        registry.broadcast(sensorId, 25.5, Instant.now());

        verify(goodEmitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(badEmitter).send(any(SseEmitter.SseEventBuilder.class));

        // Second broadcast should only reach goodEmitter
        reset(goodEmitter, badEmitter);
        registry.broadcast(sensorId, 26.0, Instant.now());
        verify(goodEmitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(badEmitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void remove_completesAllEmitters() {
        SseEmitter emitter1 = mock(SseEmitter.class);
        SseEmitter emitter2 = mock(SseEmitter.class);
        registry.register(sensorId, emitter1);
        registry.register(sensorId, emitter2);

        registry.remove(sensorId);

        verify(emitter1).complete();
        verify(emitter2).complete();
    }

    @Test
    void remove_unknownSensor_doesNotThrow() {
        assertDoesNotThrow(() -> registry.remove(UUID.randomUUID()));
    }

    @Test
    void broadcast_multipleEmitters_sendToAll() throws IOException {
        SseEmitter emitter1 = mock(SseEmitter.class);
        SseEmitter emitter2 = mock(SseEmitter.class);
        registry.register(sensorId, emitter1);
        registry.register(sensorId, emitter2);

        registry.broadcast(sensorId, 30.0, Instant.now());

        verify(emitter1).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter2).send(any(SseEmitter.SseEventBuilder.class));
    }
}
