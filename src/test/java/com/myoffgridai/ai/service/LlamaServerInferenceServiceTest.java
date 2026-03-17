package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.common.exception.EmbeddingException;
import com.myoffgridai.common.exception.OllamaInferenceException;
import com.myoffgridai.config.LlamaServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LlamaServerInferenceService}.
 *
 * <p>Mocks {@link RestClient} to avoid real HTTP calls.</p>
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class LlamaServerInferenceServiceTest {

    @Mock private RestClient restClient;
    @Mock private WebClient webClient;
    @Mock private RestClient.RequestHeadersUriSpec<?> headersUriSpec;
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RestClient.RequestBodySpec requestBodySpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private LlamaServerProperties properties;
    private LlamaServerInferenceService service;

    @BeforeEach
    void setUp() {
        properties = new LlamaServerProperties();
        properties.setPort(1234);
        properties.setActiveModel("test-model.gguf");

        service = new LlamaServerInferenceService(properties, restClient, webClient);
    }

    // ── isAvailable tests ───────────────────────────────────────────────

    @Test
    void isAvailable_returnsFalse_onConnectionError() {
        doReturn(headersUriSpec).when(restClient).get();
        doReturn(headersUriSpec).when((RestClient.RequestHeadersUriSpec) headersUriSpec).uri(anyString());
        doReturn(responseSpec).when((RestClient.RequestHeadersSpec) headersUriSpec).retrieve();
        doThrow(new ResourceAccessException("Connection refused"))
                .when(responseSpec).toBodilessEntity();

        assertFalse(service.isAvailable());
    }

    @Test
    void isAvailable_returnsTrue_onSuccess() {
        doReturn(headersUriSpec).when(restClient).get();
        doReturn(headersUriSpec).when((RestClient.RequestHeadersUriSpec) headersUriSpec).uri(anyString());
        doReturn(responseSpec).when((RestClient.RequestHeadersSpec) headersUriSpec).retrieve();
        doReturn(null).when(responseSpec).toBodilessEntity();

        assertTrue(service.isAvailable());
    }

    // ── listModels tests ────────────────────────────────────────────────

    @Test
    void listModels_returnsEmptyList_onConnectionError() {
        doReturn(headersUriSpec).when(restClient).get();
        doReturn(headersUriSpec).when((RestClient.RequestHeadersUriSpec) headersUriSpec).uri(anyString());
        doReturn(responseSpec).when((RestClient.RequestHeadersSpec) headersUriSpec).retrieve();
        doThrow(new ResourceAccessException("Connection refused"))
                .when(responseSpec).body(any(ParameterizedTypeReference.class));

        List<InferenceModelInfo> models = service.listModels();
        assertTrue(models.isEmpty());
    }

    @Test
    void listModels_parsesResponse() {
        Map<String, Object> response = Map.of(
                "data", List.of(Map.of("id", "test-model"))
        );

        doReturn(headersUriSpec).when(restClient).get();
        doReturn(headersUriSpec).when((RestClient.RequestHeadersUriSpec) headersUriSpec).uri(anyString());
        doReturn(responseSpec).when((RestClient.RequestHeadersSpec) headersUriSpec).retrieve();
        doReturn(response).when(responseSpec).body(any(ParameterizedTypeReference.class));

        List<InferenceModelInfo> models = service.listModels();
        assertEquals(1, models.size());
        assertEquals("test-model", models.getFirst().id());
    }

    // ── chat tests ──────────────────────────────────────────────────────

    @Test
    void chat_returnsResponseString() {
        Map<String, Object> apiResponse = Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", "Hello, I am an AI assistant.")
                ))
        );

        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any(MediaType.class));
        doReturn(requestBodySpec).when(requestBodySpec).body((Object) any());
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doReturn(apiResponse).when(responseSpec).body(any(ParameterizedTypeReference.class));

        List<OllamaMessage> messages = List.of(
                new OllamaMessage("user", "Hi there")
        );

        String result = service.chat(messages, UUID.randomUUID());
        assertEquals("Hello, I am an AI assistant.", result);
    }

    @Test
    void chat_throwsOnConnectionError() {
        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any(MediaType.class));
        doReturn(requestBodySpec).when(requestBodySpec).body((Object) any());
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doThrow(new ResourceAccessException("Connection refused"))
                .when(responseSpec).body(any(ParameterizedTypeReference.class));

        List<OllamaMessage> messages = List.of(
                new OllamaMessage("user", "Hi")
        );

        assertThrows(OllamaInferenceException.class,
                () -> service.chat(messages, UUID.randomUUID()));
    }

    // ── embed tests ─────────────────────────────────────────────────────

    @Test
    void embed_returnsFloatArray() {
        Map<String, Object> apiResponse = Map.of(
                "data", List.of(Map.of(
                        "embedding", List.of(0.1, 0.2, 0.3, 0.4)
                ))
        );

        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any(MediaType.class));
        doReturn(requestBodySpec).when(requestBodySpec).body((Object) any());
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doReturn(apiResponse).when(responseSpec).body(any(ParameterizedTypeReference.class));

        float[] result = service.embed("test text");

        assertEquals(4, result.length);
        assertEquals(0.1f, result[0], 0.001f);
        assertEquals(0.4f, result[3], 0.001f);
    }

    @Test
    void embed_throwsOnConnectionError() {
        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any(MediaType.class));
        doReturn(requestBodySpec).when(requestBodySpec).body((Object) any());
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doThrow(new ResourceAccessException("Connection refused"))
                .when(responseSpec).body(any(ParameterizedTypeReference.class));

        assertThrows(EmbeddingException.class, () -> service.embed("test text"));
    }

    // ── getActiveModel tests ────────────────────────────────────────────

    @Test
    void getActiveModel_fallsBackToConfigured() {
        // When listModels fails, getActiveModel returns config fallback
        doReturn(headersUriSpec).when(restClient).get();
        doReturn(headersUriSpec).when((RestClient.RequestHeadersUriSpec) headersUriSpec).uri(anyString());
        doReturn(responseSpec).when((RestClient.RequestHeadersSpec) headersUriSpec).retrieve();
        doThrow(new ResourceAccessException("Connection refused"))
                .when(responseSpec).body(any(ParameterizedTypeReference.class));

        InferenceModelInfo model = service.getActiveModel();
        assertEquals("test-model.gguf", model.id());
    }
}
