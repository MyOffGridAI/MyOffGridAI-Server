package com.myoffgridai.ai.judge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.settings.dto.ExternalApiSettingsDto;
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
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JudgeInferenceService}.
 *
 * <p>Mocks the WebClient chain and JudgeModelProcessService to validate
 * evaluation logic, JSON parsing (including markdown fencing), and
 * graceful degradation on failures.</p>
 */
@ExtendWith(MockitoExtension.class)
class JudgeInferenceServiceTest {

    @Mock private JudgeProperties judgeProperties;
    @Mock private JudgeModelProcessService judgeModelProcessService;
    @Mock private ExternalApiSettingsService externalApiSettingsService;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec requestBodySpec;
    @Mock private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private JudgeInferenceService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        service = new JudgeInferenceService(
                judgeProperties, judgeModelProcessService, externalApiSettingsService,
                objectMapper, webClientBuilder);
    }

    // ── isAvailable tests ───────────────────────────────────────────────────

    @Test
    void isAvailable_returnsTrueWhenEnabledAndRunning() {
        mockJudgeEnabled(true);
        when(judgeModelProcessService.isRunning()).thenReturn(true);

        assertTrue(service.isAvailable());
    }

    @Test
    void isAvailable_returnsFalseWhenDisabled() {
        mockJudgeEnabled(false);

        assertFalse(service.isAvailable());
    }

    @Test
    void isAvailable_returnsFalseWhenNotRunning() {
        mockJudgeEnabled(true);
        when(judgeModelProcessService.isRunning()).thenReturn(false);

        assertFalse(service.isAvailable());
    }

    // ── evaluate tests ──────────────────────────────────────────────────────

    @Test
    void evaluate_returnsEmptyWhenNotAvailable() {
        mockJudgeEnabled(false);

        Optional<JudgeResult> result = service.evaluate("query", "response");
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void evaluate_parsesValidJsonResponse() {
        mockJudgeEnabled(true);
        when(judgeModelProcessService.isRunning()).thenReturn(true);
        when(judgeModelProcessService.getPort()).thenReturn(1235);
        when(judgeProperties.getTimeoutSeconds()).thenReturn(30);

        Map<String, Object> message = Map.of("content",
                "{\"score\": 8.5, \"reason\": \"Good response\", \"needs_cloud\": false}");
        Map<String, Object> choice = Map.of("message", message);
        Map<String, Object> apiResponse = Map.of("choices", List.of(choice));

        // Wire up the WebClient mock chain
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        Mono<Map> mono = Mono.just(apiResponse);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(mono);

        Optional<JudgeResult> result = service.evaluate("What is Java?", "Java is a language.");

        assertTrue(result.isPresent());
        assertEquals(8.5, result.get().score());
        assertEquals("Good response", result.get().reason());
        assertFalse(result.get().needsCloud());
    }

    @Test
    @SuppressWarnings("unchecked")
    void evaluate_handlesMarkdownFencedJson() {
        mockJudgeEnabled(true);
        when(judgeModelProcessService.isRunning()).thenReturn(true);
        when(judgeModelProcessService.getPort()).thenReturn(1235);
        when(judgeProperties.getTimeoutSeconds()).thenReturn(30);

        String fencedJson = "```json\n{\"score\": 6.0, \"reason\": \"Needs detail\", \"needs_cloud\": true}\n```";
        Map<String, Object> message = Map.of("content", fencedJson);
        Map<String, Object> choice = Map.of("message", message);
        Map<String, Object> apiResponse = Map.of("choices", List.of(choice));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        Optional<JudgeResult> result = service.evaluate("query", "response");

        assertTrue(result.isPresent());
        assertEquals(6.0, result.get().score());
        assertTrue(result.get().needsCloud());
    }

    @Test
    @SuppressWarnings("unchecked")
    void evaluate_returnsEmptyOnNullResponse() {
        mockJudgeEnabled(true);
        when(judgeModelProcessService.isRunning()).thenReturn(true);
        when(judgeModelProcessService.getPort()).thenReturn(1235);
        when(judgeProperties.getTimeoutSeconds()).thenReturn(30);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.empty());

        Optional<JudgeResult> result = service.evaluate("query", "response");
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void evaluate_returnsEmptyOnEmptyChoices() {
        mockJudgeEnabled(true);
        when(judgeModelProcessService.isRunning()).thenReturn(true);
        when(judgeModelProcessService.getPort()).thenReturn(1235);
        when(judgeProperties.getTimeoutSeconds()).thenReturn(30);

        Map<String, Object> apiResponse = Map.of("choices", List.of());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        Optional<JudgeResult> result = service.evaluate("query", "response");
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void evaluate_returnsEmptyOnInvalidJson() {
        mockJudgeEnabled(true);
        when(judgeModelProcessService.isRunning()).thenReturn(true);
        when(judgeModelProcessService.getPort()).thenReturn(1235);
        when(judgeProperties.getTimeoutSeconds()).thenReturn(30);

        Map<String, Object> message = Map.of("content", "not valid json at all");
        Map<String, Object> choice = Map.of("message", message);
        Map<String, Object> apiResponse = Map.of("choices", List.of(choice));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        Optional<JudgeResult> result = service.evaluate("query", "response");
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void evaluate_returnsEmptyOnException() {
        mockJudgeEnabled(true);
        when(judgeModelProcessService.isRunning()).thenReturn(true);

        when(webClient.post()).thenThrow(new RuntimeException("Connection refused"));

        Optional<JudgeResult> result = service.evaluate("query", "response");
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void evaluate_handlesMissingFieldsInJson() {
        mockJudgeEnabled(true);
        when(judgeModelProcessService.isRunning()).thenReturn(true);
        when(judgeModelProcessService.getPort()).thenReturn(1235);
        when(judgeProperties.getTimeoutSeconds()).thenReturn(30);

        Map<String, Object> message = Map.of("content", "{\"score\": 7.0}");
        Map<String, Object> choice = Map.of("message", message);
        Map<String, Object> apiResponse = Map.of("choices", List.of(choice));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        Optional<JudgeResult> result = service.evaluate("query", "response");

        assertTrue(result.isPresent());
        assertEquals(7.0, result.get().score());
        assertEquals("", result.get().reason());
        assertFalse(result.get().needsCloud());
    }

    private void mockJudgeEnabled(boolean enabled) {
        ExternalApiSettingsDto dto = new ExternalApiSettingsDto(
                false, null, false, false, false, 0, 0,
                false, false, false, false, false, false, null,
                enabled, null, 7.5);
        when(externalApiSettingsService.getSettings()).thenReturn(dto);
    }
}
