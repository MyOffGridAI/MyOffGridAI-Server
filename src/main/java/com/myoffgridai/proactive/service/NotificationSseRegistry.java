package com.myoffgridai.proactive.service;

import com.myoffgridai.proactive.model.Notification;
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
 * Manages SSE connections for real-time notification push to Flutter clients.
 * Keyed by userId — each user can have multiple connected clients.
 */
@Component
public class NotificationSseRegistry {

    private static final Logger log = LoggerFactory.getLogger(NotificationSseRegistry.class);

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> registry =
            new ConcurrentHashMap<>();

    /**
     * Registers a new SSE emitter for a user and sends an immediate connected event.
     *
     * @param userId  the user ID
     * @param emitter the SSE emitter
     */
    public void register(UUID userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters =
                registry.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("Notification SSE emitter completed for user {}", userId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("Notification SSE emitter timed out for user {}", userId);
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            log.debug("Notification SSE emitter error for user {}: {}", userId, e.getMessage());
        });

        try {
            String connectedEvent = String.format(
                    "{\"connected\":true,\"userId\":\"%s\"}", userId);
            emitter.send(SseEmitter.event().data(connectedEvent));
        } catch (IOException e) {
            log.debug("Failed to send connected event to user {}", userId);
            emitters.remove(emitter);
        }

        log.debug("Registered notification SSE emitter for user {}, total: {}", userId, emitters.size());
    }

    /**
     * Broadcasts a notification to all connected emitters for a user.
     *
     * @param userId       the user ID
     * @param notification the notification to broadcast
     */
    public void broadcast(UUID userId, Notification notification) {
        CopyOnWriteArrayList<SseEmitter> emitters = registry.get(userId);
        if (emitters == null || emitters.isEmpty()) return;

        String data = String.format(
                "{\"id\":\"%s\",\"title\":\"%s\",\"body\":\"%s\",\"type\":\"%s\",\"createdAt\":\"%s\"}",
                notification.getId(),
                escapeJson(notification.getTitle()),
                escapeJson(notification.getBody()),
                notification.getType(),
                notification.getCreatedAt());

        List<SseEmitter> failed = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (IOException e) {
                log.debug("Failed to send notification SSE event to user {}", userId);
                failed.add(emitter);
            }
        }
        emitters.removeAll(failed);
    }

    /**
     * Broadcasts the unread notification count to all connected emitters for a user.
     *
     * @param userId the user ID
     * @param count  the unread notification count
     */
    public void broadcastUnreadCount(UUID userId, long count) {
        CopyOnWriteArrayList<SseEmitter> emitters = registry.get(userId);
        if (emitters == null || emitters.isEmpty()) return;

        String data = String.format("{\"unreadCount\":%d}", count);

        List<SseEmitter> failed = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (IOException e) {
                log.debug("Failed to send unread count SSE event to user {}", userId);
                failed.add(emitter);
            }
        }
        emitters.removeAll(failed);
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
