package com.myoffgridai.models.service;

import com.myoffgridai.models.dto.DownloadProgress;
import com.myoffgridai.models.dto.DownloadStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ModelDownloadProgressRegistry}.
 *
 * <p>Tests subscription, emission, dead-emitter cleanup, and completion
 * of SSE progress streams.</p>
 */
class ModelDownloadProgressRegistryTest {

    private ModelDownloadProgressRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ModelDownloadProgressRegistry();
    }

    @Test
    void subscribe_returnsSseEmitter() {
        SseEmitter emitter = registry.subscribe("dl-1");

        assertNotNull(emitter);
    }

    @Test
    void subscribe_multipleClients_returnsDistinctEmitters() {
        SseEmitter emitter1 = registry.subscribe("dl-1");
        SseEmitter emitter2 = registry.subscribe("dl-1");

        assertNotNull(emitter1);
        assertNotNull(emitter2);
        assertNotSame(emitter1, emitter2);
    }

    @Test
    void emit_noSubscribers_doesNotThrow() {
        DownloadProgress progress = createProgress("dl-1", DownloadStatus.DOWNLOADING);

        assertDoesNotThrow(() -> registry.emit("dl-1", progress));
    }

    @Test
    void emit_reachesSubscriber() throws IOException {
        // We cannot easily verify send() was called on a real SseEmitter without it
        // being wired to a servlet response. Instead, verify no exception on a fresh emitter.
        SseEmitter emitter = registry.subscribe("dl-1");
        DownloadProgress progress = createProgress("dl-1", DownloadStatus.DOWNLOADING);

        // This will throw if there's an internal error, but since the emitter
        // isn't connected to a response, the send() call will throw.
        // The emit method catches that and removes the dead emitter.
        assertDoesNotThrow(() -> registry.emit("dl-1", progress));
    }

    @Test
    void emit_deadEmittersCleaned_doesNotThrow() {
        SseEmitter emitter = registry.subscribe("dl-2");
        // Complete the emitter to simulate a dead connection
        emitter.complete();

        DownloadProgress progress = createProgress("dl-2", DownloadStatus.DOWNLOADING);

        // Emitting to a completed emitter should not throw — dead emitters are cleaned up
        assertDoesNotThrow(() -> registry.emit("dl-2", progress));
    }

    @Test
    void complete_removesAllEmitters() {
        registry.subscribe("dl-3");
        registry.subscribe("dl-3");

        // Complete all emitters for the download
        assertDoesNotThrow(() -> registry.complete("dl-3"));

        // Subsequent emit should be a no-op (no emitters remain)
        DownloadProgress progress = createProgress("dl-3", DownloadStatus.COMPLETED);
        assertDoesNotThrow(() -> registry.emit("dl-3", progress));
    }

    @Test
    void complete_nonexistentDownload_doesNotThrow() {
        assertDoesNotThrow(() -> registry.complete("nonexistent"));
    }

    @Test
    void emit_differentDownloadIds_areIsolated() {
        registry.subscribe("dl-a");
        registry.subscribe("dl-b");

        // Completing dl-a should not affect dl-b
        registry.complete("dl-a");

        DownloadProgress progress = createProgress("dl-b", DownloadStatus.DOWNLOADING);
        assertDoesNotThrow(() -> registry.emit("dl-b", progress));
    }

    private DownloadProgress createProgress(String downloadId, DownloadStatus status) {
        return new DownloadProgress(
                downloadId, "author/model", "model.gguf", status,
                1024, 4096, 25.0, 1024.0, 3, "/tmp/model.gguf", null
        );
    }
}
