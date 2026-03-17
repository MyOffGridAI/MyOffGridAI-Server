package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.ChunkType;
import com.myoffgridai.ai.dto.InferenceChunk;
import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.ai.dto.NativeLlamaStatusDto;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.config.InferenceProperties;
import com.myoffgridai.config.NativeLlamaProperties;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Sinks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NativeLlamaInferenceService}.
 *
 * <p>Tests the native java-llama.cpp inference provider including
 * think-tag parsing, ChatML formatting, embedding delegation,
 * model loading/unloading, availability checks, and status reporting.</p>
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class NativeLlamaInferenceServiceTest {

    @Mock private SystemConfigService systemConfigService;
    @Mock private RestClient ollamaEmbedRestClient;

    @TempDir
    Path tempDir;

    private InferenceProperties inferenceProperties;
    private NativeLlamaProperties nativeProperties;
    private NativeLlamaInferenceService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        inferenceProperties = new InferenceProperties();
        inferenceProperties.setModelsDir(tempDir.toString());

        nativeProperties = new NativeLlamaProperties();
        nativeProperties.setContextSize(32768);
        nativeProperties.setGpuLayers(99);
        nativeProperties.setThreads(8);
        nativeProperties.setEnableEmbedding(false);

        service = new NativeLlamaInferenceService(
                inferenceProperties, nativeProperties, systemConfigService, ollamaEmbedRestClient);
        ReflectionTestUtils.setField(service, "modelName", "test-model");
        ReflectionTestUtils.setField(service, "embedModel", "nomic-embed-text");
        ReflectionTestUtils.setField(service, "maxTokens", 4096);
        ReflectionTestUtils.setField(service, "temperature", 0.7);
        userId = UUID.randomUUID();
    }

    // ── Helper: stub RestClient POST ────────────────────────────────────────

    private RestClient.ResponseSpec stubPost(RestClient client) {
        RestClient.RequestBodyUriSpec postSpec =
                mock(RestClient.RequestBodyUriSpec.class, RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(client.post()).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        return responseSpec;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ChatML formatting tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void formatChatML_singleUserMessage() {
        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hello"));

        String result = service.formatChatML(messages);

        assertTrue(result.contains("<|im_start|>user\nhello\n<|im_end|>"));
        assertTrue(result.endsWith("<|im_start|>assistant\n"));
    }

    @Test
    void formatChatML_multipleMessages() {
        List<OllamaMessage> messages = List.of(
                new OllamaMessage("system", "You are helpful"),
                new OllamaMessage("user", "hi"),
                new OllamaMessage("assistant", "hello"),
                new OllamaMessage("user", "how are you?"));

        String result = service.formatChatML(messages);

        assertTrue(result.contains("<|im_start|>system\nYou are helpful\n<|im_end|>"));
        assertTrue(result.contains("<|im_start|>user\nhi\n<|im_end|>"));
        assertTrue(result.contains("<|im_start|>assistant\nhello\n<|im_end|>"));
        assertTrue(result.contains("<|im_start|>user\nhow are you?\n<|im_end|>"));
        assertTrue(result.endsWith("<|im_start|>assistant\n"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Think-tag state machine tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void processContentToken_emitsContentChunks() {
        Sinks.Many<InferenceChunk> sink = Sinks.many().unicast().onBackpressureBuffer();
        AtomicReference<NativeLlamaInferenceService.ThinkState> state =
                new AtomicReference<>(NativeLlamaInferenceService.ThinkState.OUTSIDE_THINK);
        StringBuilder tagBuffer = new StringBuilder();

        service.processContentToken("Hello world", state, tagBuffer, sink);

        List<InferenceChunk> chunks = new ArrayList<>();
        sink.tryEmitComplete();
        sink.asFlux().toStream().forEach(chunks::add);

        assertEquals(1, chunks.size());
        assertEquals(ChunkType.CONTENT, chunks.get(0).type());
        assertEquals("Hello world", chunks.get(0).text());
    }

    @Test
    void processContentToken_parsesThinkTags() {
        Sinks.Many<InferenceChunk> sink = Sinks.many().unicast().onBackpressureBuffer();
        AtomicReference<NativeLlamaInferenceService.ThinkState> state =
                new AtomicReference<>(NativeLlamaInferenceService.ThinkState.OUTSIDE_THINK);
        StringBuilder tagBuffer = new StringBuilder();

        service.processContentToken("<think>", state, tagBuffer, sink);
        service.processContentToken("reasoning", state, tagBuffer, sink);
        service.processContentToken("</think>", state, tagBuffer, sink);
        service.processContentToken("visible", state, tagBuffer, sink);

        List<InferenceChunk> chunks = new ArrayList<>();
        sink.tryEmitComplete();
        sink.asFlux().toStream().forEach(chunks::add);

        assertTrue(chunks.stream().anyMatch(c -> c.type() == ChunkType.THINKING
                && "reasoning".equals(c.text())));
        assertTrue(chunks.stream().anyMatch(c -> c.type() == ChunkType.CONTENT
                && "visible".equals(c.text())));
    }

    @Test
    void processContentToken_handlesSplitThinkTag() {
        Sinks.Many<InferenceChunk> sink = Sinks.many().unicast().onBackpressureBuffer();
        AtomicReference<NativeLlamaInferenceService.ThinkState> state =
                new AtomicReference<>(NativeLlamaInferenceService.ThinkState.OUTSIDE_THINK);
        StringBuilder tagBuffer = new StringBuilder();

        // Split <think> across two tokens: "<thi" + "nk>"
        service.processContentToken("<thi", state, tagBuffer, sink);
        service.processContentToken("nk>", state, tagBuffer, sink);
        service.processContentToken("split thinking", state, tagBuffer, sink);
        service.processContentToken("</think>", state, tagBuffer, sink);
        service.processContentToken("after think", state, tagBuffer, sink);

        List<InferenceChunk> chunks = new ArrayList<>();
        sink.tryEmitComplete();
        sink.asFlux().toStream().forEach(chunks::add);

        assertTrue(chunks.stream().anyMatch(c -> c.type() == ChunkType.THINKING
                && c.text().contains("split thinking")));
        assertTrue(chunks.stream().anyMatch(c -> c.type() == ChunkType.CONTENT
                && c.text().contains("after think")));
    }

    @Test
    void flushBuffer_emitsRemainingContent() {
        Sinks.Many<InferenceChunk> sink = Sinks.many().unicast().onBackpressureBuffer();
        StringBuilder tagBuffer = new StringBuilder("leftover");

        service.flushBuffer(tagBuffer, NativeLlamaInferenceService.ThinkState.OUTSIDE_THINK, sink);

        List<InferenceChunk> chunks = new ArrayList<>();
        sink.tryEmitComplete();
        sink.asFlux().toStream().forEach(chunks::add);

        assertEquals(1, chunks.size());
        assertEquals(ChunkType.CONTENT, chunks.get(0).type());
        assertEquals("leftover", chunks.get(0).text());
        assertEquals(0, tagBuffer.length());
    }

    @Test
    void flushBuffer_emitsThinkingWhenInsideThink() {
        Sinks.Many<InferenceChunk> sink = Sinks.many().unicast().onBackpressureBuffer();
        StringBuilder tagBuffer = new StringBuilder("remaining thought");

        service.flushBuffer(tagBuffer, NativeLlamaInferenceService.ThinkState.INSIDE_THINK, sink);

        List<InferenceChunk> chunks = new ArrayList<>();
        sink.tryEmitComplete();
        sink.asFlux().toStream().forEach(chunks::add);

        assertEquals(1, chunks.size());
        assertEquals(ChunkType.THINKING, chunks.get(0).type());
    }

    @Test
    void flushBuffer_emptyBuffer_doesNothing() {
        Sinks.Many<InferenceChunk> sink = Sinks.many().unicast().onBackpressureBuffer();
        StringBuilder tagBuffer = new StringBuilder();

        service.flushBuffer(tagBuffer, NativeLlamaInferenceService.ThinkState.OUTSIDE_THINK, sink);

        List<InferenceChunk> chunks = new ArrayList<>();
        sink.tryEmitComplete();
        sink.asFlux().toStream().forEach(chunks::add);

        assertTrue(chunks.isEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  partialTagMatchLength static helper tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void partialTagMatchLength_noMatch() {
        assertEquals(0, NativeLlamaInferenceService.partialTagMatchLength("hello", "<think>"));
    }

    @Test
    void partialTagMatchLength_singleChar() {
        assertEquals(1, NativeLlamaInferenceService.partialTagMatchLength("abc<", "<think>"));
    }

    @Test
    void partialTagMatchLength_multiChar() {
        assertEquals(4, NativeLlamaInferenceService.partialTagMatchLength("abc<thi", "<think>"));
    }

    @Test
    void partialTagMatchLength_almostFullTag() {
        assertEquals(6, NativeLlamaInferenceService.partialTagMatchLength("<think", "<think>"));
    }

    @Test
    void partialTagMatchLength_fullTagDoesNotMatch() {
        assertEquals(0, NativeLlamaInferenceService.partialTagMatchLength("<think>", "<think>"));
    }

    @Test
    void partialTagMatchLength_closeTag() {
        assertEquals(3, NativeLlamaInferenceService.partialTagMatchLength("end</t", "</think>"));
    }

    @Test
    void partialTagMatchLength_emptyText() {
        assertEquals(0, NativeLlamaInferenceService.partialTagMatchLength("", "<think>"));
    }

    @Test
    void partialTagMatchLength_textShorterThanTag() {
        assertEquals(2, NativeLlamaInferenceService.partialTagMatchLength("<t", "<think>"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  stripThinkTags tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void stripThinkTags_removesThinkBlocks() {
        String result = service.stripThinkTags("<think>reasoning</think>The answer is 42");
        assertEquals("The answer is 42", result);
    }

    @Test
    void stripThinkTags_handlesNull() {
        assertEquals("", service.stripThinkTags(null));
    }

    @Test
    void stripThinkTags_noThinkTags() {
        assertEquals("plain text", service.stripThinkTags("plain text"));
    }

    @Test
    void stripThinkTags_multipleBlocks() {
        String input = "<think>first</think>Hello<think>second</think> world";
        String result = service.stripThinkTags(input);
        assertEquals("Hello world", result);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  isAvailable tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void isAvailable_noModelLoaded_returnsFalse() {
        assertFalse(service.isAvailable());
    }

    @Test
    void isAvailable_modelLoadedAndReady_returnsTrue() {
        // Set up internal state to simulate a loaded model
        ReflectionTestUtils.setField(service, "model", mock(NativeLlamaModelBridge.class));
        ReflectionTestUtils.setField(service, "status", NativeLlamaStatus.READY);

        assertTrue(service.isAvailable());
    }

    @Test
    void isAvailable_modelLoadedButError_returnsFalse() {
        ReflectionTestUtils.setField(service, "model", mock(NativeLlamaModelBridge.class));
        ReflectionTestUtils.setField(service, "status", NativeLlamaStatus.ERROR);

        assertFalse(service.isAvailable());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  listModels tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void listModels_noActiveModel_returnsEmpty() {
        List<InferenceModelInfo> models = service.listModels();
        assertTrue(models.isEmpty());
    }

    @Test
    void listModels_withActiveModel_returnsSingleEntry() {
        ReflectionTestUtils.setField(service, "activeModelFilename", "test.gguf");

        List<InferenceModelInfo> models = service.listModels();

        assertEquals(1, models.size());
        assertEquals("test.gguf", models.get(0).id());
        assertEquals("gguf", models.get(0).format());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getActiveModel tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void getActiveModel_withLoadedModel_returnsModelInfo() {
        ReflectionTestUtils.setField(service, "activeModelFilename", "loaded.gguf");

        InferenceModelInfo info = service.getActiveModel();

        assertEquals("loaded.gguf", info.id());
        assertEquals("gguf", info.format());
    }

    @Test
    void getActiveModel_noLoadedModel_fallsBackToConfigured() {
        InferenceModelInfo info = service.getActiveModel();

        assertEquals("test-model", info.id());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  embed tests
    // ══════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    @Test
    void embed_delegatesToOllama() {
        Map<String, Object> response = Map.of("embedding", List.of(0.1, 0.2, 0.3, 0.4));

        RestClient.ResponseSpec resp = stubPost(ollamaEmbedRestClient);
        when(resp.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        float[] result = service.embed("test text");

        assertEquals(4, result.length);
        assertEquals(0.1f, result[0], 0.001f);
        assertEquals(0.4f, result[3], 0.001f);
        verify(ollamaEmbedRestClient).post();
    }

    @SuppressWarnings("unchecked")
    @Test
    void embed_throwsOnNullResponse() {
        RestClient.ResponseSpec resp = stubPost(ollamaEmbedRestClient);
        when(resp.body(any(ParameterizedTypeReference.class))).thenReturn(null);

        assertThrows(RuntimeException.class, () -> service.embed("test"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void embed_throwsOnMissingEmbeddingKey() {
        Map<String, Object> response = Map.of("data", "something");

        RestClient.ResponseSpec resp = stubPost(ollamaEmbedRestClient);
        when(resp.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        assertThrows(RuntimeException.class, () -> service.embed("test"));
    }

    @Test
    void embed_nativeEmbedding_usesModel() {
        nativeProperties.setEnableEmbedding(true);
        NativeLlamaModelBridge mockModel = mock(NativeLlamaModelBridge.class);
        when(mockModel.embed("test text")).thenReturn(new float[]{0.5f, 0.6f});
        ReflectionTestUtils.setField(service, "model", mockModel);

        float[] result = service.embed("test text");

        assertEquals(2, result.length);
        assertEquals(0.5f, result[0], 0.001f);
        verify(mockModel).embed("test text");
        verify(ollamaEmbedRestClient, never()).post();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  chat (sync) tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void chat_noModelLoaded_throws() {
        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hi"));
        assertThrows(RuntimeException.class, () -> service.chat(messages, userId));
    }

    @Test
    void chat_returnsStrippedContent() {
        NativeLlamaModelBridge mockModel = mock(NativeLlamaModelBridge.class);
        when(mockModel.complete(any())).thenReturn("<think>reasoning</think>The answer is 42");
        ReflectionTestUtils.setField(service, "model", mockModel);

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "what is the answer?"));
        String result = service.chat(messages, userId);

        assertEquals("The answer is 42", result);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  streamChat tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void streamChat_noModelLoaded_errors() {
        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hi"));
        reactor.test.StepVerifier.create(service.streamChat(messages, userId))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void streamChatWithThinking_noModelLoaded_errors() {
        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hi"));
        reactor.test.StepVerifier.create(service.streamChatWithThinking(messages, userId))
                .expectError(RuntimeException.class)
                .verify();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  loadModel tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void loadModel_fileNotFound_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.loadModel("nonexistent.gguf"));
    }

    @Test
    void loadModel_modelsDirNotExist_throwsIllegalArgument() {
        inferenceProperties.setModelsDir("/nonexistent/path");
        assertThrows(IllegalArgumentException.class,
                () -> service.loadModel("test.gguf"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  unloadModel tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void unloadModel_noModelLoaded_doesNothing() {
        assertDoesNotThrow(() -> service.unloadModel());
        assertEquals(NativeLlamaStatus.UNLOADED,
                ((NativeLlamaStatusDto) service.getStatus()).status());
    }

    @Test
    void unloadModel_closesModel() {
        NativeLlamaModelBridge mockModel = mock(NativeLlamaModelBridge.class);
        ReflectionTestUtils.setField(service, "model", mockModel);
        ReflectionTestUtils.setField(service, "activeModelFilename", "test.gguf");
        ReflectionTestUtils.setField(service, "status", NativeLlamaStatus.READY);

        service.unloadModel();

        verify(mockModel).close();
        assertNull(ReflectionTestUtils.getField(service, "model"));
        assertNull(ReflectionTestUtils.getField(service, "activeModelFilename"));
        assertEquals(NativeLlamaStatus.UNLOADED,
                ReflectionTestUtils.getField(service, "status"));
    }

    @Test
    void unloadModel_closeThrows_handlesGracefully() {
        NativeLlamaModelBridge mockModel = mock(NativeLlamaModelBridge.class);
        doThrow(new RuntimeException("close error")).when(mockModel).close();
        ReflectionTestUtils.setField(service, "model", mockModel);
        ReflectionTestUtils.setField(service, "activeModelFilename", "test.gguf");

        assertDoesNotThrow(() -> service.unloadModel());
        assertNull(ReflectionTestUtils.getField(service, "model"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getStatus tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void getStatus_returnsCurrentState() {
        NativeLlamaStatusDto status = service.getStatus();

        assertEquals(NativeLlamaStatus.UNLOADED, status.status());
        assertNull(status.activeModel());
        assertNull(status.errorMessage());
        assertNotNull(status.memoryUsageMb());
    }

    @Test
    void getStatus_withError_includesMessage() {
        ReflectionTestUtils.setField(service, "status", NativeLlamaStatus.ERROR);
        ReflectionTestUtils.setField(service, "errorMessage", "test error");

        NativeLlamaStatusDto status = service.getStatus();

        assertEquals(NativeLlamaStatus.ERROR, status.status());
        assertEquals("test error", status.errorMessage());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  destroy (DisposableBean) tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void destroy_unloadsModel() {
        NativeLlamaModelBridge mockModel = mock(NativeLlamaModelBridge.class);
        ReflectionTestUtils.setField(service, "model", mockModel);
        ReflectionTestUtils.setField(service, "activeModelFilename", "test.gguf");

        service.destroy();

        verify(mockModel).close();
        assertNull(ReflectionTestUtils.getField(service, "model"));
    }
}
