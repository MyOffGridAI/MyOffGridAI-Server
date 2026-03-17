package com.myoffgridai.frontier;

import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OpenAiFrontierClient}.
 */
@ExtendWith(MockitoExtension.class)
class OpenAiFrontierClientTest {

    @Mock private ExternalApiSettingsService settingsService;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec requestBodySpec;
    @Mock private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private OpenAiFrontierClient client;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        client = new OpenAiFrontierClient(settingsService, webClientBuilder);
    }

    @Test
    void getProvider_returnsOpenAi() {
        assertEquals(FrontierProvider.OPENAI, client.getProvider());
    }

    @Test
    void isAvailable_trueWhenKeyPresent() {
        when(settingsService.getOpenAiKey()).thenReturn(Optional.of("sk-key"));
        assertTrue(client.isAvailable());
    }

    @Test
    void isAvailable_falseWhenNoKey() {
        when(settingsService.getOpenAiKey()).thenReturn(Optional.empty());
        assertFalse(client.isAvailable());
    }

    @Test
    void complete_returnsEmptyWhenNoKey() {
        when(settingsService.getOpenAiKey()).thenReturn(Optional.empty());

        Optional<String> result = client.complete("system", "user");
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void complete_returnsTextOnSuccess() {
        when(settingsService.getOpenAiKey()).thenReturn(Optional.of("sk-key"));

        Map<String, Object> message = Map.of("content", "OpenAI response");
        Map<String, Object> choice = Map.of("message", message);
        Map<String, Object> apiResponse = Map.of("choices", List.of(choice));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        Optional<String> result = client.complete("system prompt", "user message");

        assertTrue(result.isPresent());
        assertEquals("OpenAI response", result.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void complete_returnsEmptyOnNullResponse() {
        when(settingsService.getOpenAiKey()).thenReturn(Optional.of("sk-key"));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.empty());

        Optional<String> result = client.complete("system", "user");
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void complete_returnsEmptyOnNoMessage() {
        when(settingsService.getOpenAiKey()).thenReturn(Optional.of("sk-key"));

        Map<String, Object> choice = Map.of("finish_reason", "stop");
        Map<String, Object> apiResponse = Map.of("choices", List.of(choice));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        Optional<String> result = client.complete("system", "user");
        assertTrue(result.isEmpty());
    }

    @Test
    void complete_returnsEmptyOnException() {
        when(settingsService.getOpenAiKey()).thenReturn(Optional.of("sk-key"));
        when(webClient.post()).thenThrow(new RuntimeException("Network error"));

        Optional<String> result = client.complete("system", "user");
        assertTrue(result.isEmpty());
    }
}
