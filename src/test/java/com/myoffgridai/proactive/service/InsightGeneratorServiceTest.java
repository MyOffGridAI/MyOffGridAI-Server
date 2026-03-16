package com.myoffgridai.proactive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.proactive.dto.PatternSummary;
import com.myoffgridai.proactive.model.Insight;
import com.myoffgridai.proactive.model.InsightCategory;
import com.myoffgridai.proactive.repository.InsightRepository;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsightGeneratorServiceTest {

    @Mock private PatternAnalysisService patternAnalysisService;
    @Mock private OllamaService ollamaService;
    @Mock private InsightRepository insightRepository;
    @Mock private NotificationService notificationService;
    @Mock private SystemConfigService systemConfigService;

    private InsightGeneratorService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new InsightGeneratorService(
                patternAnalysisService, ollamaService, insightRepository,
                notificationService, objectMapper, systemConfigService);
        userId = UUID.randomUUID();

        lenient().when(systemConfigService.getAiSettings())
                .thenReturn(new AiSettingsDto("test-model", 0.7, 0.45, 5, 2048, 4096, 20));
    }

    @Test
    void generateInsights_withData_returnsInsights() {
        PatternSummary summary = new PatternSummary(
                2, List.of("Gardening", "Solar"), List.of("Frost warning"),
                Map.of("TEMPERATURE", 5.0), List.of("Seeds"), List.of("Plant garden"), 7);
        when(patternAnalysisService.buildPatternSummary(userId)).thenReturn(summary);

        String json = "[{\"content\":\"Temperature is dropping, protect your plants\",\"category\":\"HOMESTEAD\"}]";
        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", json), true, 1000L, 50);
        when(ollamaService.chat(any())).thenReturn(response);
        when(insightRepository.save(any(Insight.class))).thenAnswer(i -> {
            Insight insight = i.getArgument(0);
            insight.setId(UUID.randomUUID());
            return insight;
        });

        List<Insight> insights = service.generateInsightForUser(userId);

        assertEquals(1, insights.size());
        assertEquals("Temperature is dropping, protect your plants", insights.get(0).getContent());
        assertEquals(InsightCategory.HOMESTEAD, insights.get(0).getCategory());
        verify(notificationService).createNotification(eq(userId), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void generateInsights_noData_returnsEmpty() {
        PatternSummary summary = new PatternSummary(
                0, List.of(), List.of(), Map.of(), List.of(), List.of(), 7);
        when(patternAnalysisService.buildPatternSummary(userId)).thenReturn(summary);

        List<Insight> insights = service.generateInsightForUser(userId);

        assertTrue(insights.isEmpty());
        verifyNoInteractions(ollamaService);
    }

    @Test
    void generateInsights_ollamaFails_returnsEmpty() {
        PatternSummary summary = new PatternSummary(
                1, List.of("Test"), List.of(), Map.of(), List.of(), List.of(), 7);
        when(patternAnalysisService.buildPatternSummary(userId)).thenReturn(summary);
        when(ollamaService.chat(any())).thenThrow(new RuntimeException("Connection refused"));

        List<Insight> insights = service.generateInsightForUser(userId);

        assertTrue(insights.isEmpty());
    }

    @Test
    void generateInsights_invalidJson_returnsEmpty() {
        PatternSummary summary = new PatternSummary(
                1, List.of("Test"), List.of(), Map.of(), List.of(), List.of(), 7);
        when(patternAnalysisService.buildPatternSummary(userId)).thenReturn(summary);

        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", "Not valid JSON at all"), true, 1000L, 50);
        when(ollamaService.chat(any())).thenReturn(response);

        List<Insight> insights = service.generateInsightForUser(userId);

        assertTrue(insights.isEmpty());
    }

    @Test
    void generateInsights_invalidCategory_defaultsToGeneral() {
        PatternSummary summary = new PatternSummary(
                1, List.of("Test"), List.of(), Map.of(), List.of(), List.of(), 7);
        when(patternAnalysisService.buildPatternSummary(userId)).thenReturn(summary);

        String json = "[{\"content\":\"Some insight\",\"category\":\"INVALID_CATEGORY\"}]";
        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", json), true, 1000L, 50);
        when(ollamaService.chat(any())).thenReturn(response);
        when(insightRepository.save(any(Insight.class))).thenAnswer(i -> {
            Insight insight = i.getArgument(0);
            insight.setId(UUID.randomUUID());
            return insight;
        });

        List<Insight> insights = service.generateInsightForUser(userId);

        assertEquals(1, insights.size());
        assertEquals(InsightCategory.GENERAL, insights.get(0).getCategory());
    }

    @Test
    void generateInsights_maxThreeInsights() {
        PatternSummary summary = new PatternSummary(
                1, List.of("Test"), List.of(), Map.of(), List.of(), List.of(), 7);
        when(patternAnalysisService.buildPatternSummary(userId)).thenReturn(summary);

        String json = "[" +
                "{\"content\":\"One\",\"category\":\"GENERAL\"}," +
                "{\"content\":\"Two\",\"category\":\"GENERAL\"}," +
                "{\"content\":\"Three\",\"category\":\"GENERAL\"}," +
                "{\"content\":\"Four\",\"category\":\"GENERAL\"}" +
                "]";
        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", json), true, 1000L, 50);
        when(ollamaService.chat(any())).thenReturn(response);
        when(insightRepository.save(any(Insight.class))).thenAnswer(i -> {
            Insight insight = i.getArgument(0);
            insight.setId(UUID.randomUUID());
            return insight;
        });

        List<Insight> insights = service.generateInsightForUser(userId);

        assertEquals(3, insights.size());
    }

    @Test
    void generateInsights_blankContent_skipped() {
        PatternSummary summary = new PatternSummary(
                1, List.of("Test"), List.of(), Map.of(), List.of(), List.of(), 7);
        when(patternAnalysisService.buildPatternSummary(userId)).thenReturn(summary);

        String json = "[{\"content\":\"\",\"category\":\"GENERAL\"},{\"content\":\"Valid\",\"category\":\"GENERAL\"}]";
        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", json), true, 1000L, 50);
        when(ollamaService.chat(any())).thenReturn(response);
        when(insightRepository.save(any(Insight.class))).thenAnswer(i -> {
            Insight insight = i.getArgument(0);
            insight.setId(UUID.randomUUID());
            return insight;
        });

        List<Insight> insights = service.generateInsightForUser(userId);

        assertEquals(1, insights.size());
        assertEquals("Valid", insights.get(0).getContent());
    }

    @Test
    void generateInsights_jsonWrappedInText_extractsArray() {
        PatternSummary summary = new PatternSummary(
                1, List.of("Test"), List.of(), Map.of(), List.of(), List.of(), 7);
        when(patternAnalysisService.buildPatternSummary(userId)).thenReturn(summary);

        String json = "Here are insights:\n[{\"content\":\"Extracted\",\"category\":\"HEALTH\"}]\nEnd.";
        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", json), true, 1000L, 50);
        when(ollamaService.chat(any())).thenReturn(response);
        when(insightRepository.save(any(Insight.class))).thenAnswer(i -> {
            Insight insight = i.getArgument(0);
            insight.setId(UUID.randomUUID());
            return insight;
        });

        List<Insight> insights = service.generateInsightForUser(userId);

        assertEquals(1, insights.size());
        assertEquals(InsightCategory.HEALTH, insights.get(0).getCategory());
    }
}
