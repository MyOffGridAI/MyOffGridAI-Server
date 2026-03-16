package com.myoffgridai.enrichment.service;

import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ClaudeApiService}.
 */
@ExtendWith(MockitoExtension.class)
class ClaudeApiServiceTest {

    @Mock
    private ExternalApiSettingsService settingsService;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ClaudeApiService claudeApiService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        claudeApiService = new ClaudeApiService(settingsService, webClientBuilder);
    }

    @Test
    void isAvailable_returnsTrueWhenKeyConfigured() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.of("test-key"));

        assertTrue(claudeApiService.isAvailable());
    }

    @Test
    void isAvailable_returnsFalseWhenNoKey() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.empty());

        assertFalse(claudeApiService.isAvailable());
    }

    @Test
    void complete_returnsEmptyWhenNoKey() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.empty());

        Optional<String> result = claudeApiService.complete("system", "user");

        assertTrue(result.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void complete_returnsTextOnSuccess() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.of("test-key"));
        when(settingsService.getAnthropicModel()).thenReturn("claude-sonnet-4-20250514");

        Map<String, Object> response = Map.of(
                "content", List.of(Map.of("type", "text", "text", "The answer is 42"))
        );

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        Optional<String> result = claudeApiService.complete("system prompt", "user prompt");

        assertTrue(result.isPresent());
        assertEquals("The answer is 42", result.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    void complete_returnsEmptyOnApiError() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.of("test-key"));
        when(settingsService.getAnthropicModel()).thenReturn("claude-sonnet-4-20250514");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.error(new WebClientResponseException(429, "Rate limited", null, null, null)));

        Optional<String> result = claudeApiService.complete("system", "user");

        assertTrue(result.isEmpty());
    }

    @Test
    void summarizeForKnowledgeBase_returnsClaudeSummaryWhenAvailable() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.of("test-key"));
        when(settingsService.getAnthropicModel()).thenReturn("claude-sonnet-4-20250514");

        Map<String, Object> response = Map.of(
                "content", List.of(Map.of("type", "text", "text", "A concise summary"))
        );

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        String result = claudeApiService.summarizeForKnowledgeBase("Long content here...", 5000);

        assertEquals("A concise summary", result);
    }

    @Test
    void summarizeForKnowledgeBase_truncatesWhenClaudeUnavailable() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.empty());

        String longContent = "A".repeat(1000);
        String result = claudeApiService.summarizeForKnowledgeBase(longContent, 500);

        assertEquals(500, result.length());
    }

    @Test
    void summarizeForKnowledgeBase_returnsOriginalWhenShortEnough() {
        when(settingsService.getAnthropicKey()).thenReturn(Optional.empty());

        String shortContent = "Short content";
        String result = claudeApiService.summarizeForKnowledgeBase(shortContent, 500);

        assertEquals(shortContent, result);
    }
}
