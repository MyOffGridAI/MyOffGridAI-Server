package com.myoffgridai.enrichment.service;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calls the Anthropic Claude API when an API key is configured in
 * {@link ExternalApiSettingsService}.
 *
 * <p>This service is optional enrichment — if no key is configured or the
 * API is unavailable, callers fall back to the local Ollama model. Claude
 * is used for: summarizing fetched web content before Knowledge Base ingestion,
 * answering questions that the local model cannot handle, and generating
 * structured content from raw web data.</p>
 */
@Service
public class ClaudeApiService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeApiService.class);

    private final ExternalApiSettingsService settingsService;
    private final WebClient webClient;

    /**
     * Constructs the Claude API service.
     *
     * @param settingsService the external API settings service
     * @param webClientBuilder the Spring WebClient builder
     */
    public ClaudeApiService(ExternalApiSettingsService settingsService,
                            WebClient.Builder webClientBuilder) {
        this.settingsService = settingsService;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Returns true if the Anthropic API is configured and enabled.
     *
     * @return true if a key is set and the feature is enabled
     */
    public boolean isAvailable() {
        return settingsService.getAnthropicKey().isPresent();
    }

    /**
     * Sends a prompt to Claude and returns the response text.
     *
     * <p>Falls back gracefully (returns {@link Optional#empty()}) if key not
     * configured, API unreachable, or rate limited.</p>
     *
     * @param systemPrompt the system prompt
     * @param userPrompt   the user prompt
     * @return the response text, or empty if unavailable
     */
    public Optional<String> complete(String systemPrompt, String userPrompt) {
        Optional<String> keyOpt = settingsService.getAnthropicKey();
        if (keyOpt.isEmpty()) {
            log.debug("Claude API not available — no key configured or disabled");
            return Optional.empty();
        }

        String model = settingsService.getAnthropicModel();

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", AppConstants.ANTHROPIC_MAX_TOKENS,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt))
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
                log.warn("Claude API returned null response");
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (content == null || content.isEmpty()) {
                log.warn("Claude API returned empty content");
                return Optional.empty();
            }

            String text = (String) content.get(0).get("text");
            return Optional.ofNullable(text);
        } catch (Exception e) {
            log.warn("Claude API call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Summarizes text content using Claude for Knowledge Base ingestion.
     *
     * <p>If Claude is unavailable, returns the original content truncated
     * to {@code maxChars}.</p>
     *
     * @param rawContent the raw text content to summarize
     * @param maxChars   the maximum character length for the result
     * @return the summarized content, or truncated original
     */
    public String summarizeForKnowledgeBase(String rawContent, int maxChars) {
        Optional<String> summary = complete(
                "You are a content summarizer. Produce a concise, information-dense "
                        + "summary suitable for a personal knowledge base. Preserve key facts, "
                        + "dates, names, and actionable information. Do not add commentary.",
                "Summarize the following content in under " + maxChars + " characters:\n\n" + rawContent
        );

        return summary.orElseGet(() -> {
            if (rawContent.length() <= maxChars) {
                return rawContent;
            }
            return rawContent.substring(0, maxChars);
        });
    }
}
