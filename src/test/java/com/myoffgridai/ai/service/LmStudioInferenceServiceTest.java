package com.myoffgridai.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.ChunkType;
import com.myoffgridai.ai.dto.InferenceChunk;
import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.config.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LmStudioInferenceService}.
 *
 * <p>Validates the LM Studio OpenAI-compatible inference provider including
 * sync chat, streaming with think-tag parsing, embedding delegation to Ollama,
 * availability checking, and model listing.</p>
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class LmStudioInferenceServiceTest {

    @Mock private WebClient lmStudioWebClient;
    @Mock private RestClient lmStudioRestClient;
    @Mock private RestClient ollamaEmbedRestClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private LmStudioInferenceService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new LmStudioInferenceService(
                lmStudioWebClient, lmStudioRestClient, ollamaEmbedRestClient, objectMapper);
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "embedModel", "nomic-embed-text");
        ReflectionTestUtils.setField(service, "maxTokens", 4096);
        ReflectionTestUtils.setField(service, "temperature", 0.7);
        userId = UUID.randomUUID();
    }

    // ── Helper: stub RestClient GET ─────────────────────────────────────────

    private RestClient.ResponseSpec stubGet(RestClient client) {
        @SuppressWarnings("unchecked")
        RestClient.RequestHeadersUriSpec<?> getSpec =
                mock(RestClient.RequestHeadersUriSpec.class, RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(getSpec).when(client).get();
        doReturn(responseSpec).when(getSpec).retrieve();
        return responseSpec;
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

    // ── Helper: stub WebClient POST returning SSE lines ─────────────────────

    private void stubWebPost(List<String> sseLines) {
        WebClient.RequestBodyUriSpec postSpec =
                mock(WebClient.RequestBodyUriSpec.class, RETURNS_SELF);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(lmStudioWebClient.post()).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(String.class)).thenReturn(Flux.fromIterable(sseLines));
    }

    // ── Helper: build a standard SSE line from content ──────────────────────

    private String sseContentLine(String content) {
        return "data: {\"choices\":[{\"delta\":{\"content\":\"" + content + "\"},\"finish_reason\":null}]}";
    }

    private String sseDoneLine(String stopReason, int completionTokens) {
        return "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"" + stopReason + "\"}],"
                + "\"usage\":{\"completion_tokens\":" + completionTokens + "}}";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  streamChatWithThinking tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void streamChatWithThinking_emitsContentChunks() {
        List<String> sseLines = List.of(
                sseContentLine("Hello"),
                sseContentLine(" world"),
                sseDoneLine("stop", 2),
                "data: [DONE]");
        stubWebPost(sseLines);

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hi"));

        StepVerifier.create(service.streamChatWithThinking(messages, userId))
                .expectNextMatches(c -> c.type() == ChunkType.CONTENT && "Hello".equals(c.text()))
                .expectNextMatches(c -> c.type() == ChunkType.CONTENT && " world".equals(c.text()))
                .expectNextMatches(c -> c.type() == ChunkType.DONE && c.metadata() != null)
                .verifyComplete();
    }

    @Test
    void streamChatWithThinking_emitsThinkingChunks() {
        List<String> sseLines = List.of(
                sseContentLine("<think>"),
                sseContentLine("reasoning here"),
                sseContentLine("</think>"),
                sseContentLine("visible answer"),
                sseDoneLine("stop", 4),
                "data: [DONE]");
        stubWebPost(sseLines);

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "think about this"));

        List<InferenceChunk> chunks = new ArrayList<>();
        StepVerifier.create(service.streamChatWithThinking(messages, userId))
                .recordWith(() -> chunks)
                .thenConsumeWhile(c -> true)
                .verifyComplete();

        // Verify we got THINKING, CONTENT, and DONE chunks
        assertTrue(chunks.stream().anyMatch(c -> c.type() == ChunkType.THINKING
                && c.text().contains("reasoning here")));
        assertTrue(chunks.stream().anyMatch(c -> c.type() == ChunkType.CONTENT
                && c.text().contains("visible answer")));
        assertTrue(chunks.stream().anyMatch(c -> c.type() == ChunkType.DONE));
    }

    @Test
    void streamChatWithThinking_handlesSplitThinkTag() {
        // The <think> tag is split across two SSE chunks: "<thi" then "nk>"
        List<String> sseLines = List.of(
                sseContentLine("<thi"),
                sseContentLine("nk>"),
                sseContentLine("split thinking"),
                sseContentLine("</think>"),
                sseContentLine("after think"),
                sseDoneLine("stop", 5),
                "data: [DONE]");
        stubWebPost(sseLines);

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "split test"));

        List<InferenceChunk> chunks = new ArrayList<>();
        StepVerifier.create(service.streamChatWithThinking(messages, userId))
                .recordWith(() -> chunks)
                .thenConsumeWhile(c -> true)
                .verifyComplete();

        // The split tag should be handled: we should see THINKING content
        assertTrue(chunks.stream().anyMatch(c -> c.type() == ChunkType.THINKING
                && c.text().contains("split thinking")));
        assertTrue(chunks.stream().anyMatch(c -> c.type() == ChunkType.CONTENT
                && c.text().contains("after think")));
    }

    @Test
    void streamChatWithThinking_emitsDoneWithMetadata() {
        List<String> sseLines = List.of(
                sseContentLine("token1"),
                sseDoneLine("length", 10),
                "data: [DONE]");
        stubWebPost(sseLines);

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "test"));

        List<InferenceChunk> chunks = new ArrayList<>();
        StepVerifier.create(service.streamChatWithThinking(messages, userId))
                .recordWith(() -> chunks)
                .thenConsumeWhile(c -> true)
                .verifyComplete();

        InferenceChunk doneChunk = chunks.stream()
                .filter(c -> c.type() == ChunkType.DONE)
                .findFirst()
                .orElseThrow();

        assertNotNull(doneChunk.metadata());
        assertEquals(10, doneChunk.metadata().tokensGenerated());
        assertEquals("length", doneChunk.metadata().stopReason());
        assertTrue(doneChunk.metadata().inferenceTimeSeconds() >= 0);
        assertTrue(doneChunk.metadata().tokensPerSecond() >= 0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  isAvailable tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void isAvailable_returnsTrueOn200() {
        RestClient.ResponseSpec resp = stubGet(lmStudioRestClient);
        when(resp.body(String.class)).thenReturn("{\"data\":[]}");

        assertTrue(service.isAvailable());
    }

    @Test
    void isAvailable_returnsFalseOnError() {
        RestClient.ResponseSpec resp = stubGet(lmStudioRestClient);
        when(resp.body(String.class)).thenThrow(new RuntimeException("Connection refused"));

        assertFalse(service.isAvailable());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  listModels tests
    // ══════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    @Test
    void listModels_parsesResponse() {
        Map<String, Object> response = Map.of("data", List.of(
                Map.of("id", "qwen3.5-27b", "created", 1700000000L),
                Map.of("id", "llama3-8b", "created", 1700001000L)));

        RestClient.ResponseSpec resp = stubGet(lmStudioRestClient);
        when(resp.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        List<InferenceModelInfo> models = service.listModels();

        assertEquals(2, models.size());
        assertEquals("qwen3.5-27b", models.get(0).id());
        assertEquals("qwen3.5-27b", models.get(0).name());
        assertEquals("gguf", models.get(0).format());
        assertNotNull(models.get(0).modifiedAt());
        assertEquals("llama3-8b", models.get(1).id());
    }

    @SuppressWarnings("unchecked")
    @Test
    void listModels_returnsEmptyOnNullData() {
        Map<String, Object> response = Map.of("object", "list");

        RestClient.ResponseSpec resp = stubGet(lmStudioRestClient);
        when(resp.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        List<InferenceModelInfo> models = service.listModels();
        assertTrue(models.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void listModels_returnsEmptyOnError() {
        RestClient.ResponseSpec resp = stubGet(lmStudioRestClient);
        when(resp.body(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("timeout"));

        List<InferenceModelInfo> models = service.listModels();
        assertTrue(models.isEmpty());
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
        assertEquals(0.2f, result[1], 0.001f);
        assertEquals(0.3f, result[2], 0.001f);
        assertEquals(0.4f, result[3], 0.001f);

        // Verify it used the ollamaEmbedRestClient, not lmStudioRestClient
        verify(ollamaEmbedRestClient).post();
        verify(lmStudioRestClient, never()).post();
    }

    @SuppressWarnings("unchecked")
    @Test
    void embed_throwsOnNullResponse() {
        RestClient.ResponseSpec resp = stubPost(ollamaEmbedRestClient);
        when(resp.body(any(ParameterizedTypeReference.class))).thenReturn(null);

        assertThrows(RuntimeException.class, () -> service.embed("test"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  chat (sync) tests
    // ══════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    @Test
    void chat_returnsStrippedContent() {
        Map<String, Object> response = Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("content", "<think>reasoning</think>The answer is 42"))));

        RestClient.ResponseSpec resp = stubPost(lmStudioRestClient);
        when(resp.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "what is the answer?"));
        String result = service.chat(messages, userId);

        assertEquals("The answer is 42", result);
    }

    @SuppressWarnings("unchecked")
    @Test
    void chat_throwsOnNullResponse() {
        RestClient.ResponseSpec resp = stubPost(lmStudioRestClient);
        when(resp.body(any(ParameterizedTypeReference.class))).thenReturn(null);

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hi"));
        assertThrows(RuntimeException.class, () -> service.chat(messages, userId));
    }

    @SuppressWarnings("unchecked")
    @Test
    void chat_throwsOnEmptyChoices() {
        Map<String, Object> response = Map.of("choices", List.of());

        RestClient.ResponseSpec resp = stubPost(lmStudioRestClient);
        when(resp.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "hi"));
        assertThrows(RuntimeException.class, () -> service.chat(messages, userId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  streamChat tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void streamChat_filtersToContentOnly() {
        List<String> sseLines = List.of(
                sseContentLine("<think>"),
                sseContentLine("thinking"),
                sseContentLine("</think>"),
                sseContentLine("visible"),
                sseDoneLine("stop", 4),
                "data: [DONE]");
        stubWebPost(sseLines);

        List<OllamaMessage> messages = List.of(new OllamaMessage("user", "test"));

        // streamChat should only emit CONTENT text strings, not THINKING or DONE
        List<String> tokens = new ArrayList<>();
        StepVerifier.create(service.streamChat(messages, userId))
                .recordWith(() -> tokens)
                .thenConsumeWhile(t -> true)
                .verifyComplete();

        // Should contain "visible" but not "thinking"
        assertTrue(tokens.stream().anyMatch(t -> t.contains("visible")));
        assertFalse(tokens.stream().anyMatch(t -> t.contains("thinking")));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getActiveModel tests
    // ══════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    @Test
    void getActiveModel_returnsFirstModel() {
        Map<String, Object> response = Map.of("data", List.of(
                Map.of("id", "loaded-model", "created", 1700000000L)));

        RestClient.ResponseSpec resp = stubGet(lmStudioRestClient);
        when(resp.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        InferenceModelInfo active = service.getActiveModel();
        assertEquals("loaded-model", active.id());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getActiveModel_fallsBackToConfiguredModel() {
        RestClient.ResponseSpec resp = stubGet(lmStudioRestClient);
        when(resp.body(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("unavailable"));

        InferenceModelInfo active = service.getActiveModel();
        assertEquals("test-model", active.id());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  partialTagMatchLength static helper tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void partialTagMatchLength_noMatch() {
        assertEquals(0, LmStudioInferenceService.partialTagMatchLength("hello", "<think>"));
    }

    @Test
    void partialTagMatchLength_singleChar() {
        assertEquals(1, LmStudioInferenceService.partialTagMatchLength("abc<", "<think>"));
    }

    @Test
    void partialTagMatchLength_multiChar() {
        assertEquals(4, LmStudioInferenceService.partialTagMatchLength("abc<thi", "<think>"));
    }

    @Test
    void partialTagMatchLength_almostFullTag() {
        assertEquals(6, LmStudioInferenceService.partialTagMatchLength("<think", "<think>"));
    }

    @Test
    void partialTagMatchLength_fullTagDoesNotMatch() {
        // Full tag match returns 0 because maxLen = min(text.len, tag.len - 1)
        // and indexOf would find the tag, not partialTagMatchLength
        assertEquals(0, LmStudioInferenceService.partialTagMatchLength("<think>", "<think>"));
    }

    @Test
    void partialTagMatchLength_closeTag() {
        assertEquals(3, LmStudioInferenceService.partialTagMatchLength("end</t", "</think>"));
    }

    @Test
    void partialTagMatchLength_emptyText() {
        assertEquals(0, LmStudioInferenceService.partialTagMatchLength("", "<think>"));
    }

    @Test
    void partialTagMatchLength_textShorterThanTag() {
        assertEquals(2, LmStudioInferenceService.partialTagMatchLength("<t", "<think>"));
    }
}
