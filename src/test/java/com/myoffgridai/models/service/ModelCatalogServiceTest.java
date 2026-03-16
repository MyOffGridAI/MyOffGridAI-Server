package com.myoffgridai.models.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.models.dto.HfModelDto;
import com.myoffgridai.models.dto.HfModelFileDto;
import com.myoffgridai.models.dto.HfSearchResultDto;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ModelCatalogService}.
 *
 * <p>Tests HuggingFace model search, detail retrieval, and file listing
 * by mocking the WebClient chain and verifying correct JSON parsing,
 * auth header inclusion, and error handling.</p>
 */
@ExtendWith(MockitoExtension.class)
class ModelCatalogServiceTest {

    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;
    @Mock private ExternalApiSettingsService settingsService;
    @Mock private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
    @Mock private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private ModelCatalogService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        // Default: get() returns the chain
        WebClient.RequestHeadersUriSpec rawSpec = requestHeadersUriSpec;
        when(webClient.get()).thenReturn(rawSpec);

        service = new ModelCatalogService(webClientBuilder, settingsService, objectMapper);
    }

    // ── searchModels ────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void searchModels_deserializesCorrectly() {
        String json = """
                [
                  {
                    "id": "TheBloke/Llama-2-7B-GGUF",
                    "downloads": 50000,
                    "likes": 300,
                    "tags": ["text-generation", "gguf"],
                    "pipeline_tag": "text-generation",
                    "gated": false,
                    "siblings": [
                      {"rfilename": "model-Q4_K_M.gguf", "size": 4000000000}
                    ]
                  }
                ]
                """;

        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(json));

        HfSearchResultDto result = service.searchModels("llama", "gguf", 20);

        assertNotNull(result);
        assertEquals(1, result.totalCount());
        assertEquals(1, result.models().size());

        HfModelDto model = result.models().get(0);
        assertEquals("TheBloke/Llama-2-7B-GGUF", model.id());
        assertEquals("Llama-2-7B-GGUF", model.modelId());
        assertEquals("TheBloke", model.author());
        assertEquals(50000, model.downloads());
        assertEquals(300, model.likes());
        assertEquals("text-generation", model.pipelineTag());
        assertFalse(model.gated());
        assertEquals(1, model.siblings().size());
        assertEquals("model-Q4_K_M.gguf", model.siblings().get(0).rfilename());
        assertEquals(4000000000L, model.siblings().get(0).size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchModels_authHeaderAddedWhenTokenPresent() {
        String json = "[]";

        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.of("hf_test_token_123"));
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        doReturn(requestHeadersSpec).when(requestHeadersSpec)
                .header(eq("Authorization"), eq("Bearer hf_test_token_123"));
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(json));

        HfSearchResultDto result = service.searchModels("test", "gguf", 10);

        assertNotNull(result);
        verify(requestHeadersSpec).header("Authorization", "Bearer hf_test_token_123");
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchModels_429_throwsHuggingFaceRateLimitException() {
        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenThrow(
                WebClientResponseException.create(429, "Too Many Requests",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0], StandardCharsets.UTF_8));

        assertThrows(ModelCatalogService.HuggingFaceRateLimitException.class,
                () -> service.searchModels("test", "gguf", 10));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchModels_403_throwsHuggingFaceAccessDeniedException() {
        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenThrow(
                WebClientResponseException.create(403, "Forbidden",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0], StandardCharsets.UTF_8));

        assertThrows(ModelCatalogService.HuggingFaceAccessDeniedException.class,
                () -> service.searchModels("gated-model", "gguf", 10));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchModels_otherError_throwsRuntimeException() {
        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenThrow(
                new RuntimeException("Connection refused"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.searchModels("test", "gguf", 10));
        assertTrue(ex.getMessage().contains("Failed to search HuggingFace"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchModels_clampsLimitToMaximum() {
        String json = "[]";

        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(json));

        // Passing 200 should be clamped to 50 (HF_SEARCH_MAX_LIMIT)
        HfSearchResultDto result = service.searchModels("test", "gguf", 200);

        assertNotNull(result);
        assertEquals(0, result.totalCount());
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchModels_emptyArrayResponse_returnsEmptyResult() {
        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("[]"));

        HfSearchResultDto result = service.searchModels("nonexistent", "gguf", 10);

        assertNotNull(result);
        assertEquals(0, result.totalCount());
        assertTrue(result.models().isEmpty());
    }

    // ── getModelDetails ─────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getModelDetails_returnsModel() {
        String json = """
                {
                  "id": "TheBloke/Llama-2-7B-GGUF",
                  "downloads": 50000,
                  "likes": 300,
                  "tags": ["gguf"],
                  "pipeline_tag": "text-generation",
                  "gated": false,
                  "siblings": []
                }
                """;

        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(json));

        HfModelDto model = service.getModelDetails("TheBloke/Llama-2-7B-GGUF");

        assertNotNull(model);
        assertEquals("TheBloke/Llama-2-7B-GGUF", model.id());
        assertEquals("TheBloke", model.author());
        assertEquals("Llama-2-7B-GGUF", model.modelId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getModelDetails_429_throwsRateLimitException() {
        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenThrow(
                WebClientResponseException.create(429, "Too Many Requests",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0], StandardCharsets.UTF_8));

        assertThrows(ModelCatalogService.HuggingFaceRateLimitException.class,
                () -> service.getModelDetails("author/model"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getModelDetails_403_throwsAccessDeniedException() {
        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenThrow(
                WebClientResponseException.create(403, "Forbidden",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0], StandardCharsets.UTF_8));

        assertThrows(ModelCatalogService.HuggingFaceAccessDeniedException.class,
                () -> service.getModelDetails("gated/model"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getModelDetails_otherError_throwsRuntimeException() {
        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenThrow(new RuntimeException("Network error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getModelDetails("author/model"));
        assertTrue(ex.getMessage().contains("Failed to get model details"));
    }

    // ── getModelFiles ───────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getModelFiles_returnsFiles() {
        String json = """
                {
                  "id": "TheBloke/Llama-2-7B-GGUF",
                  "siblings": [
                    {"rfilename": "model-Q4_K_M.gguf", "size": 4000000000, "lfs": {"oid": "abc123"}},
                    {"rfilename": "model-Q5_K_M.gguf", "size": 5000000000}
                  ]
                }
                """;

        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(json));

        List<HfModelFileDto> files = service.getModelFiles("TheBloke/Llama-2-7B-GGUF");

        assertNotNull(files);
        assertEquals(2, files.size());
        assertEquals("model-Q4_K_M.gguf", files.get(0).rfilename());
        assertEquals(4000000000L, files.get(0).size());
        assertEquals("abc123", files.get(0).blobId());
        assertEquals("model-Q5_K_M.gguf", files.get(1).rfilename());
        assertNull(files.get(1).blobId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getModelFiles_noSiblings_returnsEmptyList() {
        String json = """
                {
                  "id": "author/empty-model"
                }
                """;

        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(json));

        List<HfModelFileDto> files = service.getModelFiles("author/empty-model");

        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getModelFiles_429_throwsRateLimitException() {
        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenThrow(
                WebClientResponseException.create(429, "Too Many Requests",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0], StandardCharsets.UTF_8));

        assertThrows(ModelCatalogService.HuggingFaceRateLimitException.class,
                () -> service.getModelFiles("author/model"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getModelFiles_403_throwsAccessDeniedException() {
        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenThrow(
                WebClientResponseException.create(403, "Forbidden",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0], StandardCharsets.UTF_8));

        assertThrows(ModelCatalogService.HuggingFaceAccessDeniedException.class,
                () -> service.getModelFiles("gated/model"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getModelFiles_otherError_throwsRuntimeException() {
        when(settingsService.getHuggingFaceToken()).thenReturn(Optional.empty());
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenThrow(new RuntimeException("Timeout"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getModelFiles("author/model"));
        assertTrue(ex.getMessage().contains("Failed to get model files"));
    }
}
