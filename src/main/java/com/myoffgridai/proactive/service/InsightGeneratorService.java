package com.myoffgridai.proactive.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.OllamaChatRequest;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.proactive.dto.PatternSummary;
import com.myoffgridai.system.service.SystemConfigService;
import com.myoffgridai.proactive.model.Insight;
import com.myoffgridai.proactive.model.InsightCategory;
import com.myoffgridai.proactive.model.NotificationType;
import com.myoffgridai.proactive.repository.InsightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates proactive insights by analyzing user activity patterns
 * and prompting the Ollama LLM for actionable recommendations.
 */
@Service
public class InsightGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(InsightGeneratorService.class);

    private final PatternAnalysisService patternAnalysisService;
    private final OllamaService ollamaService;
    private final InsightRepository insightRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfigService;

    /**
     * Constructs the insight generator service.
     *
     * @param patternAnalysisService the pattern analysis service
     * @param ollamaService          the Ollama LLM service
     * @param insightRepository      the insight repository
     * @param notificationService    the notification service
     * @param objectMapper           the JSON object mapper
     * @param systemConfigService    the system config service for dynamic AI settings
     */
    public InsightGeneratorService(PatternAnalysisService patternAnalysisService,
                                   OllamaService ollamaService,
                                   InsightRepository insightRepository,
                                   NotificationService notificationService,
                                   ObjectMapper objectMapper,
                                   SystemConfigService systemConfigService) {
        this.patternAnalysisService = patternAnalysisService;
        this.ollamaService = ollamaService;
        this.insightRepository = insightRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.systemConfigService = systemConfigService;
    }

    /**
     * Generates insights for a user after validating the user exists.
     *
     * @param userId the user ID
     * @return list of generated insights
     */
    public List<Insight> generateInsightForUser(UUID userId) {
        return generateInsights(userId);
    }

    /**
     * Generates insights by analyzing patterns and prompting Ollama.
     *
     * @param userId the user ID
     * @return list of generated and persisted insights
     */
    List<Insight> generateInsights(UUID userId) {
        PatternSummary summary = patternAnalysisService.buildPatternSummary(userId);

        if (!summary.hasData()) {
            log.debug("No meaningful data for insight generation for user {}", userId);
            return List.of();
        }

        String prompt = buildPrompt(summary);
        OllamaChatRequest request = new OllamaChatRequest(
                systemConfigService.getAiSettings().modelName(),
                List.of(new OllamaMessage("user", prompt)),
                false,
                null
        );

        String responseText;
        try {
            OllamaChatResponse response = ollamaService.chat(request);
            responseText = response.message() != null ? response.message().content() : "";
        } catch (Exception e) {
            log.warn("Ollama call failed during insight generation for user {}: {}", userId, e.getMessage());
            return List.of();
        }

        List<Map<String, String>> parsed = parseInsightJson(responseText);
        if (parsed.isEmpty()) {
            return List.of();
        }

        List<Insight> insights = new ArrayList<>();
        int limit = Math.min(parsed.size(), AppConstants.MAX_INSIGHTS_PER_GENERATION);
        for (int i = 0; i < limit; i++) {
            Map<String, String> item = parsed.get(i);
            String content = item.get("content");
            String categoryStr = item.getOrDefault("category", "GENERAL");

            if (content == null || content.isBlank()) continue;

            InsightCategory category;
            try {
                category = InsightCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                category = InsightCategory.GENERAL;
            }

            Insight insight = new Insight();
            insight.setUserId(userId);
            insight.setContent(content);
            insight.setCategory(category);
            insights.add(insightRepository.save(insight));
        }

        if (!insights.isEmpty()) {
            notificationService.createNotification(
                    userId,
                    "New Insights Ready",
                    "You have " + insights.size() + " new insight" + (insights.size() > 1 ? "s" : "") + " ready",
                    NotificationType.INSIGHT_READY,
                    null
            );
            log.info("Generated {} insights for user {}", insights.size(), userId);
        }

        return insights;
    }

    private String buildPrompt(PatternSummary summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a proactive homestead AI assistant. Based on the following activity summary, ");
        sb.append("generate 1-3 specific, actionable insights for this user. Focus on patterns that ");
        sb.append("suggest upcoming needs, risks, or opportunities.\n\n");
        sb.append("Activity Summary:\n");
        sb.append("- Recent conversations (").append(summary.recentConversationCount()).append("): ");
        sb.append(String.join(", ", summary.recentConversationTitles())).append("\n");
        sb.append("- Key memories: ").append(String.join("; ", summary.highImportanceMemories())).append("\n");
        sb.append("- Sensor averages (last 24h): ");
        sb.append(summary.sensorAverages().entrySet().stream()
                .map(e -> e.getKey() + "=" + String.format("%.1f", e.getValue()))
                .collect(Collectors.joining(", "))).append("\n");
        sb.append("- Low stock items: ").append(String.join(", ", summary.lowStockItems())).append("\n");
        sb.append("- Active tasks: ").append(String.join(", ", summary.activeTasks())).append("\n\n");
        sb.append("Generate insights as a JSON array. Each insight must have:\n");
        sb.append("- \"content\": specific actionable insight (1-2 sentences)\n");
        sb.append("- \"category\": one of HOMESTEAD, HEALTH, RESOURCE, GENERAL\n\n");
        sb.append("Return ONLY the JSON array, no other text. If there are no meaningful insights, return [].");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseInsightJson(String responseText) {
        try {
            String trimmed = responseText.trim();
            int start = trimmed.indexOf('[');
            int end = trimmed.lastIndexOf(']');
            if (start == -1 || end == -1 || end <= start) {
                log.warn("No JSON array found in insight response");
                return List.of();
            }
            String jsonArray = trimmed.substring(start, end + 1);
            return objectMapper.readValue(jsonArray, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse insight JSON response: {}", e.getMessage());
            return List.of();
        }
    }
}
