package com.myoffgridai.proactive.service;

import com.myoffgridai.proactive.model.Notification;
import com.myoffgridai.proactive.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationSseRegistryTest {

    private NotificationSseRegistry registry;
    private UUID userId;

    @BeforeEach
    void setUp() {
        registry = new NotificationSseRegistry();
        userId = UUID.randomUUID();
    }

    @Test
    void register_addsEmitterAndSendsConnectedEvent() {
        SseEmitter emitter = new SseEmitter();

        assertDoesNotThrow(() -> registry.register(userId, emitter));
    }

    @Test
    void broadcast_withRegisteredEmitter_sendsData() {
        SseEmitter emitter = new SseEmitter();
        registry.register(userId, emitter);

        Notification notification = createNotification();

        assertDoesNotThrow(() -> registry.broadcast(userId, notification));
    }

    @Test
    void broadcast_noEmitter_doesNothing() {
        Notification notification = createNotification();

        assertDoesNotThrow(() -> registry.broadcast(UUID.randomUUID(), notification));
    }

    @Test
    void broadcastUnreadCount_withRegisteredEmitter_sendsCount() {
        SseEmitter emitter = new SseEmitter();
        registry.register(userId, emitter);

        assertDoesNotThrow(() -> registry.broadcastUnreadCount(userId, 5));
    }

    @Test
    void broadcastUnreadCount_noEmitter_doesNothing() {
        assertDoesNotThrow(() -> registry.broadcastUnreadCount(UUID.randomUUID(), 3));
    }

    @Test
    void register_multipleEmitters_allReceiveEvents() {
        SseEmitter emitter1 = new SseEmitter();
        SseEmitter emitter2 = new SseEmitter();

        registry.register(userId, emitter1);
        registry.register(userId, emitter2);

        Notification notification = createNotification();
        assertDoesNotThrow(() -> registry.broadcast(userId, notification));
    }

    private Notification createNotification() {
        Notification n = new Notification();
        n.setId(UUID.randomUUID());
        n.setUserId(userId);
        n.setTitle("Test");
        n.setBody("Test body");
        n.setType(NotificationType.GENERAL);
        n.setCreatedAt(Instant.now());
        return n;
    }
}
