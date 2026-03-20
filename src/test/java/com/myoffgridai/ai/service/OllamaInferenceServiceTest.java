package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.ChunkType;
import com.myoffgridai.ai.dto.InferenceChunk;
import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.ai.dto.OllamaChatChunk;
import com.myoffgridai.ai.dto.OllamaChatRequest;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.dto.OllamaModelInfo;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OllamaInferenceService}.
 *
 * <p>Validates the Ollama inference wrapper that adapts {@link OllamaService}
 * to the {@link InferenceService} interface. Tests delegation patterns, model
 * listing mapping, and streaming chunk classification.</p>
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class OllamaInferenceServiceTest {

    @Mock private OllamaService ollamaService;
    @Mock private SystemConfigService systemConfigService;

    private OllamaInferenceService service;
    private UUID userId;
    private AiSettingsDto defaultSettings;

    @BeforeEach
    void setUp() {
        service = new OllamaInferenceService(
                ollamaService, systemConfigService, "default-model", "nomic-embed-text");
        userId = UUID.randomUUID();
        defaultSettings = new AiSettingsDto("test-model", 0.7, 0.45, 5, 2048, 4096, 20);
        lenient().when(systemConfigService.getAiSettings()).thenReturn(defaultSettings);
        // Default: model does not support native thinking (safe fallback)
        lenient().when(ollamaService.getModelCapabilities(anyString())).thenReturn(List.of("completion"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  chat tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void chat_delegatesToOllamaService() {
        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", "Hello!"), true, 1000L, 5);
        when(ollamaService.chat(any(OllamaChatRequest.class))).thenReturn(response);

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hi"));
        String result = service.chat(messages, userId);

        assertEquals("Hello!", result);
        verify(ollamaService).chat(any(OllamaChatRequest.class));
    }

    @Test
    void chat_usesAiSettingsModelName() {
        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", "response"), true, 1000L, 5);
        when(ollamaService.chat(any(OllamaChatRequest.class))).thenReturn(response);

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hi"));
        service.chat(messages, userId);

        ArgumentCaptor<OllamaChatRequest> captor = ArgumentCaptor.forClass(OllamaChatRequest.class);
        verify(ollamaService).chat(captor.capture());
        assertEquals("test-model", captor.getValue().model());
        assertFalse(captor.getValue().stream());
    }

    @Test
    void chat_fallsBackToDefaultModelWhenSettingsNull() {
        when(systemConfigService.getAiSettings())
                .thenReturn(new AiSettingsDto(null, 0.7, 0.45, 5, 2048, 4096, 20));

        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", "response"), true, 1000L, 5);
        when(ollamaService.chat(any(OllamaChatRequest.class))).thenReturn(response);

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hi"));
        service.chat(messages, userId);

        ArgumentCaptor<OllamaChatRequest> captor = ArgumentCaptor.forClass(OllamaChatRequest.class);
        verify(ollamaService).chat(captor.capture());
        assertEquals("default-model", captor.getValue().model());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  streamChat tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void streamChat_delegatesToOllamaService() {
        OllamaChatChunk chunk1 = new OllamaChatChunk(new OllamaMessage("assistant", "Hi"), false);
        OllamaChatChunk chunk2 = new OllamaChatChunk(new OllamaMessage("assistant", " there"), false);
        OllamaChatChunk doneChunk = new OllamaChatChunk(null, true);
        when(ollamaService.chatStream(any(OllamaChatRequest.class)))
                .thenReturn(Flux.just(chunk1, chunk2, doneChunk));

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hello"));

        StepVerifier.create(service.streamChat(messages, userId))
                .expectNext("Hi")
                .expectNext(" there")
                .verifyComplete();

        verify(ollamaService).chatStream(any(OllamaChatRequest.class));
    }

    @Test
    void streamChat_filtersNullMessages() {
        OllamaChatChunk nullMsg = new OllamaChatChunk(null, false);
        OllamaChatChunk chunk = new OllamaChatChunk(new OllamaMessage("assistant", "data"), false);
        OllamaChatChunk doneChunk = new OllamaChatChunk(null, true);
        when(ollamaService.chatStream(any(OllamaChatRequest.class)))
                .thenReturn(Flux.just(nullMsg, chunk, doneChunk));

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hello"));

        StepVerifier.create(service.streamChat(messages, userId))
                .expectNext("data")
                .verifyComplete();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  streamChatWithThinking tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void streamChatWithThinking_emitsContentChunks() {
        OllamaChatChunk chunk1 = new OllamaChatChunk(new OllamaMessage("assistant", "Hello"), false);
        OllamaChatChunk chunk2 = new OllamaChatChunk(new OllamaMessage("assistant", " world"), false);
        OllamaChatChunk doneChunk = new OllamaChatChunk(null, true);
        when(ollamaService.chatStream(any(OllamaChatRequest.class)))
                .thenReturn(Flux.just(chunk1, chunk2, doneChunk));

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hello"));

        List<InferenceChunk> chunks = new ArrayList<>();
        StepVerifier.create(service.streamChatWithThinking(messages, userId))
                .recordWith(() -> chunks)
                .thenConsumeWhile(c -> true)
                .verifyComplete();

        assertEquals(3, chunks.size());
        assertEquals(ChunkType.CONTENT, chunks.get(0).type());
        assertEquals("Hello", chunks.get(0).text());
        assertEquals(ChunkType.CONTENT, chunks.get(1).type());
        assertEquals(" world", chunks.get(1).text());
        assertEquals(ChunkType.DONE, chunks.get(2).type());
    }

    @Test
    void streamChatWithThinking_emitsThinkingFromThinkingField() {
        // Ollama 0.6+ sends thinking content in a dedicated 'thinking' field
        OllamaChatChunk think1 = new OllamaChatChunk(
                new OllamaMessage("assistant", "", "Let me"), false);
        OllamaChatChunk think2 = new OllamaChatChunk(
                new OllamaMessage("assistant", "", " reason"), false);
        OllamaChatChunk content1 = new OllamaChatChunk(
                new OllamaMessage("assistant", "The answer"), false);
        OllamaChatChunk doneChunk = new OllamaChatChunk(null, true);
        when(ollamaService.chatStream(any(OllamaChatRequest.class)))
                .thenReturn(Flux.just(think1, think2, content1, doneChunk));

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "think about this"));

        List<InferenceChunk> chunks = new ArrayList<>();
        StepVerifier.create(service.streamChatWithThinking(messages, userId))
                .recordWith(() -> chunks)
                .thenConsumeWhile(c -> true)
                .verifyComplete();

        assertEquals(4, chunks.size());
        assertEquals(ChunkType.THINKING, chunks.get(0).type());
        assertEquals("Let me", chunks.get(0).text());
        assertEquals(ChunkType.THINKING, chunks.get(1).type());
        assertEquals(" reason", chunks.get(1).text());
        assertEquals(ChunkType.CONTENT, chunks.get(2).type());
        assertEquals("The answer", chunks.get(2).text());
        assertEquals(ChunkType.DONE, chunks.get(3).type());
    }

    @Test
    void streamChatWithThinking_doneChunkHasMetadata() {
        OllamaChatChunk chunk = new OllamaChatChunk(new OllamaMessage("assistant", "text"), false);
        OllamaChatChunk doneChunk = new OllamaChatChunk(null, true);
        when(ollamaService.chatStream(any(OllamaChatRequest.class)))
                .thenReturn(Flux.just(chunk, doneChunk));

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hello"));

        List<InferenceChunk> chunks = new ArrayList<>();
        StepVerifier.create(service.streamChatWithThinking(messages, userId))
                .recordWith(() -> chunks)
                .thenConsumeWhile(c -> true)
                .verifyComplete();

        InferenceChunk done = chunks.stream()
                .filter(c -> c.type() == ChunkType.DONE)
                .findFirst()
                .orElseThrow();

        assertNotNull(done.metadata());
        assertEquals(1, done.metadata().tokensGenerated());
        assertEquals("stop", done.metadata().stopReason());
        assertTrue(done.metadata().inferenceTimeSeconds() >= 0);
    }

    @Test
    void streamChatWithThinking_skipsEmptyContentChunks() {
        // Chunks with null message or empty content/thinking are silently skipped
        // by the handle() operator (no sink.next() call).
        OllamaChatChunk validChunk = new OllamaChatChunk(
                new OllamaMessage("assistant", "valid"), false);
        OllamaChatChunk doneChunk = new OllamaChatChunk(null, true);

        when(ollamaService.chatStream(any(OllamaChatRequest.class)))
                .thenReturn(Flux.just(validChunk, doneChunk));

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hello"));

        List<InferenceChunk> chunks = new ArrayList<>();
        StepVerifier.create(service.streamChatWithThinking(messages, userId))
                .recordWith(() -> chunks)
                .thenConsumeWhile(c -> true)
                .verifyComplete();

        // Only "valid" CONTENT and DONE should pass through
        long contentCount = chunks.stream().filter(c -> c.type() == ChunkType.CONTENT).count();
        assertEquals(1, contentCount);
        assertEquals("valid", chunks.get(0).text());
        assertEquals(ChunkType.DONE, chunks.get(1).type());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  thinking capability auto-detection tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void streamChatWithThinking_modelSupportsThinking_sendsThinkTrue() {
        when(ollamaService.getModelCapabilities("test-model"))
                .thenReturn(List.of("completion", "thinking"));
        // Need a fresh service so the cache is empty
        service = new OllamaInferenceService(
                ollamaService, systemConfigService, "default-model", "nomic-embed-text");

        OllamaChatChunk chunk = new OllamaChatChunk(new OllamaMessage("assistant", "ok"), false);
        OllamaChatChunk done = new OllamaChatChunk(null, true);
        when(ollamaService.chatStream(any(OllamaChatRequest.class)))
                .thenReturn(Flux.just(chunk, done));

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hello"));
        StepVerifier.create(service.streamChatWithThinking(messages, userId))
                .thenConsumeWhile(c -> true)
                .verifyComplete();

        ArgumentCaptor<OllamaChatRequest> captor = ArgumentCaptor.forClass(OllamaChatRequest.class);
        verify(ollamaService).chatStream(captor.capture());
        assertEquals(Boolean.TRUE, captor.getValue().think());
    }

    @Test
    void streamChatWithThinking_modelDoesNotSupportThinking_omitsThink() {
        when(ollamaService.getModelCapabilities("test-model"))
                .thenReturn(List.of("completion"));
        // Need a fresh service so the cache is empty
        service = new OllamaInferenceService(
                ollamaService, systemConfigService, "default-model", "nomic-embed-text");

        OllamaChatChunk chunk = new OllamaChatChunk(new OllamaMessage("assistant", "ok"), false);
        OllamaChatChunk done = new OllamaChatChunk(null, true);
        when(ollamaService.chatStream(any(OllamaChatRequest.class)))
                .thenReturn(Flux.just(chunk, done));

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hello"));
        StepVerifier.create(service.streamChatWithThinking(messages, userId))
                .thenConsumeWhile(c -> true)
                .verifyComplete();

        ArgumentCaptor<OllamaChatRequest> captor = ArgumentCaptor.forClass(OllamaChatRequest.class);
        verify(ollamaService).chatStream(captor.capture());
        assertNull(captor.getValue().think());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  embed tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void embed_delegatesToOllamaService() {
        float[] expected = {0.1f, 0.2f, 0.3f};
        when(ollamaService.embed("test text")).thenReturn(expected);

        float[] result = service.embed("test text");

        assertArrayEquals(expected, result);
        verify(ollamaService).embed("test text");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  isAvailable tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void isAvailable_delegatesToOllamaService() {
        when(ollamaService.isAvailable()).thenReturn(true);
        assertTrue(service.isAvailable());

        when(ollamaService.isAvailable()).thenReturn(false);
        assertFalse(service.isAvailable());

        verify(ollamaService, times(2)).isAvailable();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  listModels tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void listModels_mapsToInferenceModelInfo() {
        Instant now = Instant.now();
        List<OllamaModelInfo> ollamaModels = List.of(
                new OllamaModelInfo("llama3:8b", 4_000_000_000L, now),
                new OllamaModelInfo("qwen:7b", 3_500_000_000L, now));
        when(ollamaService.listModels()).thenReturn(ollamaModels);

        List<InferenceModelInfo> result = service.listModels();

        assertEquals(2, result.size());

        assertEquals("llama3:8b", result.get(0).id());
        assertEquals("llama3:8b", result.get(0).name());
        assertEquals(4_000_000_000L, result.get(0).sizeBytes());
        assertNull(result.get(0).format());
        assertEquals(now, result.get(0).modifiedAt());

        assertEquals("qwen:7b", result.get(1).id());
        assertEquals("qwen:7b", result.get(1).name());
        assertEquals(3_500_000_000L, result.get(1).sizeBytes());
    }

    @Test
    void listModels_returnsEmptyWhenOllamaEmpty() {
        when(ollamaService.listModels()).thenReturn(List.of());

        List<InferenceModelInfo> result = service.listModels();
        assertTrue(result.isEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getActiveModel tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void getActiveModel_returnsConfiguredModel() {
        InferenceModelInfo active = service.getActiveModel();
        assertEquals("default-model", active.id());
        assertEquals("default-model", active.name());
        assertNull(active.sizeBytes());
        assertNull(active.format());
        assertNull(active.modifiedAt());
    }
}
