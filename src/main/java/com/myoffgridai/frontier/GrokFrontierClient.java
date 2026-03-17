package com.myoffgridai.frontier;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Frontier client for the xAI Grok API.
 *
 * <p>Uses an OpenAI-compatible chat completions endpoint at
 * {@value AppConstants#GROK_API_BASE_URL}. Requires a Grok API key
 * to be configured and enabled in external API settings.</p>
 */
@Component
public class GrokFrontierClient implements FrontierApiClient {

    private static final Logger log = LoggerFactory.getLogger(GrokFrontierClient.class);

    private final ExternalApiSettingsService settingsService;
    private final WebClient webClient;

    /**
     * Constructs the Grok frontier client.
     *
     * @param settingsService  external API settings for key lookup
     * @param webClientBuilder Spring WebClient builder
     */
    public GrokFrontierClient(ExternalApiSettingsService settingsService,
                               WebClient.Builder webClientBuilder) {
        this.settingsService = settingsService;
        this.webClient = webClientBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FrontierProvider getProvider() {
        return FrontierProvider.GROK;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Available when Grok is enabled and a key is configured.</p>
     */
    @Override
    public boolean isAvailable() {
        return settingsService.getGrokKey().isPresent();
    }

    /**
     * {@inheritDoc}
     *
     * <p>POSTs to the Grok chat completions API using OpenAI-compatible format.</p>
     */
    @Override
    public Optional<String> complete(String systemPrompt, String userMessage) {
        Optional<String> keyOpt = settingsService.getGrokKey();
        if (keyOpt.isEmpty()) {
            log.debug("Grok frontier not available — no key configured");
            return Optional.empty();
        }

        Map<String, Object> body = Map.of(
                "model", AppConstants.GROK_DEFAULT_MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", 2048
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(AppConstants.GROK_API_BASE_URL + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + keyOpt.get())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            return extractOpenAiContent(response);

        } catch (Exception e) {
            log.warn("Grok frontier call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extractOpenAiContent(Map<String, Object> response) {
        if (response == null) {
            log.warn("Grok frontier returned null response");
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            log.warn("Grok frontier returned no choices");
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((String) message.get("content"));
    }
}
