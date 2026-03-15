package com.myoffgridai.skills.builtin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.OllamaChatRequest;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.model.DocumentStatus;
import com.myoffgridai.knowledge.model.KnowledgeChunk;
import com.myoffgridai.knowledge.service.KnowledgeService;
import com.myoffgridai.skills.service.BuiltInSkill;
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Built-in skill that summarizes knowledge documents using Ollama inference.
 *
 * <p>Fetches chunks from a specified knowledge document and generates
 * an executive summary, key points, and action items.</p>
 */
@Component
public class DocumentSummarizerSkill implements BuiltInSkill {

    private static final Logger log = LoggerFactory.getLogger(DocumentSummarizerSkill.class);

    private final KnowledgeService knowledgeService;
    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfigService;

    /**
     * Constructs the document summarizer skill.
     *
     * @param knowledgeService    the knowledge service
     * @param ollamaService       the Ollama service for inference
     * @param objectMapper        the JSON object mapper
     * @param systemConfigService the system config service for dynamic AI settings
     */
    public DocumentSummarizerSkill(KnowledgeService knowledgeService,
                                    OllamaService ollamaService,
                                    ObjectMapper objectMapper,
                                    SystemConfigService systemConfigService) {
        this.knowledgeService = knowledgeService;
        this.ollamaService = ollamaService;
        this.objectMapper = objectMapper;
        this.systemConfigService = systemConfigService;
    }

    @Override
    public String getSkillName() {
        return AppConstants.SKILL_DOCUMENT_SUMMARIZER;
    }

    /**
     * Summarizes a knowledge document.
     *
     * @param userId the user's ID
     * @param params must contain "documentId" (UUID string)
     * @return map with documentName, summary, keyPoints, actionItems, chunksAnalyzed
     */
    @Override
    public Map<String, Object> execute(UUID userId, Map<String, Object> params) {
        String documentIdStr = (String) params.get("documentId");
        if (documentIdStr == null || documentIdStr.isBlank()) {
            throw new IllegalArgumentException("documentId parameter is required");
        }

        UUID documentId = UUID.fromString(documentIdStr);
        log.info("Summarizing document {} for user {}", documentId, userId);

        KnowledgeDocumentDto doc = knowledgeService.getDocument(documentId, userId);

        if (doc.status() != DocumentStatus.READY) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Document is not ready for summarization. Current status: " + doc.status());
            error.put("documentName", doc.displayName() != null ? doc.displayName() : doc.filename());
            return error;
        }

        List<KnowledgeChunk> chunks = knowledgeService.getChunks(documentId, userId);
        int maxChunks = Math.min(chunks.size(), AppConstants.SUMMARIZER_MAX_CHUNKS);
        List<KnowledgeChunk> selectedChunks = chunks.subList(0, maxChunks);

        String chunkContent = selectedChunks.stream()
                .map(KnowledgeChunk::getContent)
                .collect(Collectors.joining("\n\n"));

        String filename = doc.displayName() != null ? doc.displayName() : doc.filename();

        String prompt = """
                Summarize the following document titled "%s":

                %s

                Provide: a 2-3 sentence executive summary, 5 key points, and any action items if applicable.
                Format as JSON: {"summary": "...", "keyPoints": ["..."], "actionItems": ["..."]}
                Respond ONLY with the JSON object."""
                .formatted(filename, chunkContent);

        OllamaChatRequest request = new OllamaChatRequest(
                systemConfigService.getAiSettings().modelName(),
                List.of(new OllamaMessage("user", prompt)),
                false, Map.of());

        OllamaChatResponse response = ollamaService.chat(request);
        String responseText = response.message().content();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentName", filename);

        try {
            Map<String, Object> parsed = objectMapper.readValue(
                    responseText, new TypeReference<>() {});
            result.putAll(parsed);
        } catch (Exception e) {
            log.warn("Failed to parse summary JSON, returning raw text");
            result.put("summary", responseText);
            result.put("keyPoints", List.of());
            result.put("actionItems", List.of());
        }

        result.put("chunksAnalyzed", maxChunks);
        return result;
    }
}
