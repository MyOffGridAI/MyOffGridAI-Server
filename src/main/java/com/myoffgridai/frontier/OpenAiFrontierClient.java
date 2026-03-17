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
 * Frontier client for the OpenAI API.
 *
 * <p>Sends chat completion requests to the standard OpenAI endpoint at
 * {@value AppConstants#OPENAI_API_BASE_URL}. Requires an OpenAI API key
 * to be configured and enabled in external API settings.</p>
 */
@Component
public class OpenAiFrontierClient implements FrontierApiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiFrontierClient.class);

    private final ExternalApiSettingsService settingsService;
    private final WebClient webClient;

    /**
     * Constructs the OpenAI frontier client.
     *
     * @param settingsService  external API settings for key lookup
     * @param webClientBuilder Spring WebClient builder
     */
    public OpenAiFrontierClient(ExternalApiSettingsService settingsService,
                                 WebClient.Builder webClientBuilder) {
        this.settingsService = settingsService;
        this.webClient = webClientBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FrontierProvider getProvider() {
        return FrontierProvider.OPENAI;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Available when OpenAI is enabled and a key is configured.</p>
     */
    @Override
    public boolean isAvailable() {
        return settingsService.getOpenAiKey().isPresent();
    }

    /**
     * {@inheritDoc}
     *
     * <p>POSTs to the OpenAI chat completions API.</p>
     */
    @Override
    public Optional<String> complete(String systemPrompt, String userMessage) {
        Optional<String> keyOpt = settingsService.getOpenAiKey();
        if (keyOpt.isEmpty()) {
            log.debug("OpenAI frontier not available — no key configured");
            return Optional.empty();
        }

        Map<String, Object> body = Map.of(
                "model", AppConstants.OPENAI_DEFAULT_MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", 2048
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(AppConstants.OPENAI_API_BASE_URL + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + keyOpt.get())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            return extractOpenAiContent(response);

        } catch (Exception e) {
            log.warn("OpenAI frontier call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extractOpenAiContent(Map<String, Object> response) {
        if (response == null) {
            log.warn("OpenAI frontier returned null response");
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            log.warn("OpenAI frontier returned no choices");
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
