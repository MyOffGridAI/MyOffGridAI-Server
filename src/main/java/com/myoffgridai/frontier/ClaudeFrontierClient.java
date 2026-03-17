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
 * Frontier client for the Anthropic Claude API.
 *
 * <p>Sends completion requests to the Anthropic Messages API using the
 * configured API key and model. This is a self-contained implementation
 * that does not depend on the enrichment {@code ClaudeApiService}.</p>
 */
@Component
public class ClaudeFrontierClient implements FrontierApiClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeFrontierClient.class);

    private final ExternalApiSettingsService settingsService;
    private final WebClient webClient;

    /**
     * Constructs the Claude frontier client.
     *
     * @param settingsService  external API settings for key and model lookup
     * @param webClientBuilder Spring WebClient builder
     */
    public ClaudeFrontierClient(ExternalApiSettingsService settingsService,
                                 WebClient.Builder webClientBuilder) {
        this.settingsService = settingsService;
        this.webClient = webClientBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FrontierProvider getProvider() {
        return FrontierProvider.CLAUDE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Available when Anthropic is enabled and a key is configured.</p>
     */
    @Override
    public boolean isAvailable() {
        return settingsService.getAnthropicKey().isPresent();
    }

    /**
     * {@inheritDoc}
     *
     * <p>POSTs to the Anthropic Messages API with the configured model
     * and a 2048 max_tokens limit.</p>
     */
    @Override
    public Optional<String> complete(String systemPrompt, String userMessage) {
        Optional<String> keyOpt = settingsService.getAnthropicKey();
        if (keyOpt.isEmpty()) {
            log.debug("Claude frontier not available — no key configured");
            return Optional.empty();
        }

        String model = settingsService.getAnthropicModel();

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 2048,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userMessage))
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(AppConstants.ANTHROPIC_API_URL)
                    .header("x-api-key", keyOpt.get())
                    .header("anthropic-version", AppConstants.ANTHROPIC_API_VERSION)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(AppConstants.ANTHROPIC_TIMEOUT_SECONDS))
                    .block();

            if (response == null) {
                log.warn("Claude frontier returned null response");
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (content == null || content.isEmpty()) {
                log.warn("Claude frontier returned empty content");
                return Optional.empty();
            }

            String text = (String) content.get(0).get("text");
            return Optional.ofNullable(text);

        } catch (Exception e) {
            log.warn("Claude frontier call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
