package com.myoffgridai.models.service;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.models.dto.DownloadProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE emitter registry for streaming download progress to connected clients.
 *
 * <p>Clients subscribe to a download's progress stream via
 * {@code GET /api/models/download/{downloadId}/progress}. Each subscribed
 * client receives JSON-formatted {@link DownloadProgress} events until
 * the download completes, fails, or is cancelled.</p>
 */
@Component
public class ModelDownloadProgressRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelDownloadProgressRegistry.class);

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    /**
     * Subscribes a client to progress updates for a download.
     *
     * @param downloadId the download identifier
     * @return a new SSE emitter for the client
     */
    public SseEmitter subscribe(String downloadId) {
        SseEmitter emitter = new SseEmitter(
                (long) AppConstants.HF_DOWNLOAD_TIMEOUT_MINUTES * 60 * 1000);

        emitters.computeIfAbsent(downloadId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(downloadId, emitter));
        emitter.onTimeout(() -> removeEmitter(downloadId, emitter));
        emitter.onError(e -> removeEmitter(downloadId, emitter));

        log.debug("Client subscribed to download progress: {}", downloadId);
        return emitter;
    }

    /**
     * Emits a progress update to all subscribed clients for a download.
     *
     * @param downloadId the download identifier
     * @param progress   the current progress state
     */
    public void emit(String downloadId, DownloadProgress progress) {
        List<SseEmitter> list = emitters.get(downloadId);
        if (list == null || list.isEmpty()) return;

        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(progress));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }

    /**
     * Completes all emitters for a download and removes them from the registry.
     *
     * @param downloadId the download identifier
     */
    public void complete(String downloadId) {
        List<SseEmitter> list = emitters.remove(downloadId);
        if (list != null) {
            for (SseEmitter emitter : list) {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // Emitter may already be closed
                }
            }
            log.debug("Completed all emitters for download: {}", downloadId);
        }
    }

    private void removeEmitter(String downloadId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(downloadId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
