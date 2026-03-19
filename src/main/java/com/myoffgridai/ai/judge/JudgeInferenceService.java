package com.myoffgridai.ai.judge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sends evaluation prompts to the judge llama-server and parses scored results.
 *
 * <p>All failures are handled gracefully — if the judge is unavailable,
 * the scoring prompt fails, or the JSON cannot be parsed, an empty
 * {@link Optional} is returned and the caller proceeds without judge input.</p>
 */
@Service
public class JudgeInferenceService {

    private static final Logger log = LoggerFactory.getLogger(JudgeInferenceService.class);

    private final JudgeProperties judgeProperties;
    private final JudgeModelProcessService judgeModelProcessService;
    private final ExternalApiSettingsService externalApiSettingsService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    /**
     * Constructs the judge inference service.
     *
     * @param judgeProperties            judge configuration
     * @param judgeModelProcessService   the judge process manager
     * @param externalApiSettingsService DB-backed settings for judge enabled flag
     * @param objectMapper               Jackson object mapper for JSON parsing
     * @param webClientBuilder           Spring WebClient builder
     */
    public JudgeInferenceService(JudgeProperties judgeProperties,
                                  JudgeModelProcessService judgeModelProcessService,
                                  ExternalApiSettingsService externalApiSettingsService,
                                  ObjectMapper objectMapper,
                                  WebClient.Builder webClientBuilder) {
        this.judgeProperties = judgeProperties;
        this.judgeModelProcessService = judgeModelProcessService;
        this.externalApiSettingsService = externalApiSettingsService;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Returns whether the judge pipeline is both enabled and the process is running.
     *
     * <p>Checks the database-backed setting (set via the Settings UI) rather than
     * the YAML property, so toggling the judge in the UI takes effect immediately.</p>
     *
     * @return true if the judge is ready to evaluate responses
     */
    public boolean isAvailable() {
        return externalApiSettingsService.getSettings().judgeEnabled()
                && judgeModelProcessService.isRunning();
    }

    /**
     * Evaluates an assistant response for a given user query using the judge model.
     *
     * <p>Sends a scoring prompt to the judge llama-server and parses the JSON
     * response. Returns {@link Optional#empty()} on any failure (network error,
     * parse failure, process unavailable).</p>
     *
     * @param userQuery         the original user query
     * @param assistantResponse the assistant's response to evaluate
     * @return the parsed judge result, or empty on failure
     */
    public Optional<JudgeResult> evaluate(String userQuery, String assistantResponse) {
        if (!isAvailable()) {
            log.debug("Judge not available — skipping evaluation");
            return Optional.empty();
        }

        String scoringPrompt = buildScoringPrompt(userQuery, assistantResponse);

        Map<String, Object> body = Map.of(
                "model", "judge",
                "messages", List.of(Map.of("role", "user", "content", scoringPrompt)),
                "max_tokens", 128,
                "temperature", 0.0
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("http://localhost:" + judgeModelProcessService.getPort() + "/v1/chat/completions")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(judgeProperties.getTimeoutSeconds()))
                    .block();

            if (response == null) {
                log.warn("Judge returned null response");
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("Judge returned no choices");
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                log.warn("Judge returned no message in choice");
                return Optional.empty();
            }

            String content = (String) message.get("content");
            return parseJudgeJson(content);

        } catch (Exception e) {
            log.warn("Judge evaluation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private String buildScoringPrompt(String userQuery, String assistantResponse) {
        return "System: You are a response quality evaluator. Score the assistant response for the given user query.\n"
                + "Respond ONLY with a valid JSON object in this exact format:\n"
                + "{\"score\": <number 1-10>, \"reason\": \"<brief explanation>\", \"needs_cloud\": <true|false>}\n\n"
                + "Scoring guide:\n"
                + "- correctness: Is the information accurate?\n"
                + "- completeness: Does it fully address the query?\n"
                + "- relevance: Is it on-topic?\n"
                + "- confidence: How confident are you in the above?\n\n"
                + "Set needs_cloud=true if any of: score < 7, the query requires real-time data, specialized professional advice\n"
                + "(medical/legal/financial), or the local model clearly lacks knowledge on the topic.\n\n"
                + "User: " + userQuery + "\n"
                + "Assistant: " + assistantResponse;
    }

    private Optional<JudgeResult> parseJudgeJson(String content) {
        if (content == null || content.isBlank()) {
            log.warn("Judge returned empty content");
            return Optional.empty();
        }

        try {
            // Strip any markdown fencing if present
            String json = content.trim();
            if (json.startsWith("```")) {
                int start = json.indexOf('\n');
                int end = json.lastIndexOf("```");
                if (start >= 0 && end > start) {
                    json = json.substring(start + 1, end).trim();
                }
            }

            JsonNode node = objectMapper.readTree(json);

            double score = node.has("score") ? node.get("score").asDouble() : 0.0;
            String reason = node.has("reason") ? node.get("reason").asText() : "";
            boolean needsCloud = node.has("needs_cloud") && node.get("needs_cloud").asBoolean();

            return Optional.of(new JudgeResult(score, reason, needsCloud));

        } catch (Exception e) {
            log.warn("Failed to parse judge JSON: {} — raw content: {}", e.getMessage(), content);
            return Optional.empty();
        }
    }
}
