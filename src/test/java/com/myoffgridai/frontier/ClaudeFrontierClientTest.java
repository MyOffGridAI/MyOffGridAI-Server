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
 * Unit tests for {@link ClaudeFrontierClient}.
 */
@ExtendWith(MockitoExtension.class)
class ClaudeFrontierClientTest {

    @Mock private ExternalApiSettingsService settingsService;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec requestBodySpec;
    @Mock private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private ClaudeFrontierClient client;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        client = new ClaudeFrontierClient(settingsService, webClientBuilder);
    }

    @Test
    void getProvider_returnsClaude() {
        assertEquals(FrontierProvider.CLAUDE, client.getProvider());
    }

    @Test
    void isAvailable_trueWhenKeyPresent() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.of("key"));
        assertTrue(client.isAvailable());
    }

    @Test
    void isAvailable_falseWhenNoKey() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.empty());
        assertFalse(client.isAvailable());
    }

    @Test
    void complete_returnsEmptyWhenNoKey() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.empty());

        Optional<String> result = client.complete("system", "user");
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void complete_returnsTextOnSuccess() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.of("test-key"));
        when(settingsService.getAnthropicModel()).thenReturn("claude-sonnet-4-20250514");

        Map<String, Object> contentBlock = Map.of("type", "text", "text", "Enhanced response");
        Map<String, Object> apiResponse = Map.of("content", List.of(contentBlock));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        Optional<String> result = client.complete("system prompt", "user message");

        assertTrue(result.isPresent());
        assertEquals("Enhanced response", result.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void complete_returnsEmptyOnNullResponse() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.of("test-key"));
        when(settingsService.getAnthropicModel()).thenReturn("claude-sonnet-4-20250514");

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
    void complete_returnsEmptyOnEmptyContent() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.of("test-key"));
        when(settingsService.getAnthropicModel()).thenReturn("claude-sonnet-4-20250514");

        Map<String, Object> apiResponse = Map.of("content", List.of());

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
        when(settingsService.getAnthropicKey()).thenReturn(Optional.of("test-key"));
        when(settingsService.getAnthropicModel()).thenReturn("claude-sonnet-4-20250514");
        when(webClient.post()).thenThrow(new RuntimeException("Connection refused"));

        Optional<String> result = client.complete("system", "user");
        assertTrue(result.isEmpty());
    }
}
