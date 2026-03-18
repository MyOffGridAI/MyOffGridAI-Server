package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.*;
import com.myoffgridai.common.exception.OllamaInferenceException;
import com.myoffgridai.common.exception.OllamaUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class OllamaServiceTest {

    @Mock private RestClient restClient;
    @Mock private WebClient webClient;

    private OllamaService ollamaService;

    @BeforeEach
    void setUp() {
        ollamaService = new OllamaService(restClient, webClient, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    // Helpers that create RETURNS_SELF mocks for fluent APIs
    private RestClient.ResponseSpec stubGet() {
        @SuppressWarnings("unchecked")
        RestClient.RequestHeadersUriSpec<?> getSpec =
                mock(RestClient.RequestHeadersUriSpec.class, RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(getSpec).when(restClient).get();
        doReturn(responseSpec).when(getSpec).retrieve();
        return responseSpec;
    }

    private RestClient.ResponseSpec stubPost() {
        RestClient.RequestBodyUriSpec postSpec =
                mock(RestClient.RequestBodyUriSpec.class, RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        return responseSpec;
    }

    private WebClient.ResponseSpec stubWebPost() {
        WebClient.RequestBodyUriSpec postSpec =
                mock(WebClient.RequestBodyUriSpec.class, RETURNS_SELF);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        return responseSpec;
    }

    // ── isAvailable tests ────────────────────────────────────────────────

    @Test
    void isAvailable_returnsTrue_when200() {
        RestClient.ResponseSpec resp = stubGet();
        when(resp.body(String.class)).thenReturn("ok");

        assertTrue(ollamaService.isAvailable());
    }

    @Test
    void isAvailable_returnsFalse_onConnectionRefused() {
        RestClient.ResponseSpec resp = stubGet();
        when(resp.body(String.class)).thenThrow(new RuntimeException("Connection refused"));

        assertFalse(ollamaService.isAvailable());
    }

    @Test
    void isAvailable_returnsFalse_onServerError() {
        RestClient.ResponseSpec resp = stubGet();
        when(resp.body(String.class)).thenThrow(new RuntimeException("500"));

        assertFalse(ollamaService.isAvailable());
    }

    // ── chat tests ───────────────────────────────────────────────────────

    @Test
    void chat_success_returnsResponse() {
        OllamaChatRequest request = new OllamaChatRequest(
                "test-model", List.of(new OllamaMessage("user", "hello")), false, Map.of());
        OllamaChatResponse expected = new OllamaChatResponse(
                new OllamaMessage("assistant", "Hi there!"), true, 1000L, 5);

        RestClient.ResponseSpec resp = stubPost();
        when(resp.body(OllamaChatResponse.class)).thenReturn(expected);

        OllamaChatResponse result = ollamaService.chat(request);
        assertNotNull(result);
        assertEquals("Hi there!", result.message().content());
    }

    @Test
    void chat_throwsOllamaUnavailable_onConnectionError() {
        OllamaChatRequest request = new OllamaChatRequest(
                "test-model", List.of(new OllamaMessage("user", "hello")), false, Map.of());

        RestClient.ResponseSpec resp = stubPost();
        when(resp.body(OllamaChatResponse.class))
                .thenThrow(new RuntimeException("I/O error on POST request"));

        assertThrows(OllamaUnavailableException.class, () -> ollamaService.chat(request));
    }

    @Test
    void chat_throwsOllamaInference_onErrorResponse() {
        OllamaChatRequest request = new OllamaChatRequest(
                "test-model", List.of(new OllamaMessage("user", "hello")), false, Map.of());

        RestClient.ResponseSpec resp = stubPost();
        when(resp.body(OllamaChatResponse.class))
                .thenThrow(new RuntimeException("model not found"));

        assertThrows(OllamaInferenceException.class, () -> ollamaService.chat(request));
    }

    // ── chatStream tests ─────────────────────────────────────────────────

    @Test
    void chatStream_emitsChunks_completesOnDone() {
        OllamaChatRequest request = new OllamaChatRequest(
                "test-model", List.of(new OllamaMessage("user", "hello")), true, Map.of());

        String json1 = "{\"message\":{\"role\":\"assistant\",\"content\":\"Hi\"},\"done\":false}";
        String json2 = "{\"message\":{\"role\":\"assistant\",\"content\":\" there!\"},\"done\":true}";

        WebClient.ResponseSpec resp = stubWebPost();
        when(resp.bodyToFlux(String.class)).thenReturn(Flux.just(json1, json2));

        Flux<OllamaChatChunk> result = ollamaService.chatStream(request);

        StepVerifier.create(result)
                .assertNext(c -> {
                    assertEquals("Hi", c.message().content());
                    assertFalse(c.done());
                })
                .assertNext(c -> {
                    assertEquals(" there!", c.message().content());
                    assertTrue(c.done());
                })
                .verifyComplete();
    }

    // ── embed tests ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    @Test
    void embed_returnsFloatArray() {
        Map<String, Object> response = Map.of("embedding", List.of(0.1, 0.2, 0.3));

        RestClient.ResponseSpec resp = stubPost();
        when(resp.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        float[] result = ollamaService.embed("test text");
        assertEquals(3, result.length);
        assertEquals(0.1f, result[0], 0.001);
    }

    @Test
    void embed_throwsOnUnavailable() {
        RestClient.ResponseSpec resp = stubPost();
        when(resp.body(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThrows(OllamaUnavailableException.class, () -> ollamaService.embed("test"));
    }

    // ── listModels tests ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    @Test
    void listModels_returnsModelList() {
        Map<String, Object> response = Map.of("models", List.of(
                Map.of("name", "llama3:8b", "size", 4_000_000_000L,
                        "modified_at", Instant.now().toString())
        ));

        RestClient.ResponseSpec resp = stubGet();
        when(resp.body(any(ParameterizedTypeReference.class))).thenReturn(response);

        List<OllamaModelInfo> result = ollamaService.listModels();
        assertEquals(1, result.size());
        assertEquals("llama3:8b", result.get(0).name());
    }
}
