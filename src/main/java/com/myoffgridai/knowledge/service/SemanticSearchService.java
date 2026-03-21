package com.myoffgridai.knowledge.service;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.knowledge.dto.KnowledgeSearchResultDto;
import com.myoffgridai.knowledge.dto.SemanticSearchResult;
import com.myoffgridai.knowledge.model.KnowledgeChunk;
import com.myoffgridai.knowledge.model.KnowledgeDocument;
import com.myoffgridai.knowledge.repository.KnowledgeChunkRepository;
import com.myoffgridai.knowledge.repository.KnowledgeDocumentRepository;
import com.myoffgridai.memory.model.VectorDocument;
import com.myoffgridai.memory.model.VectorSourceType;
import com.myoffgridai.memory.repository.VectorDocumentRepository;
import com.myoffgridai.memory.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Performs semantic search across a user's knowledge base using vector
 * similarity. Retrieves the most relevant knowledge chunks for a given
 * query and enriches results with source document metadata.
 *
 * <p>This service is used both by {@link com.myoffgridai.knowledge.controller.KnowledgeController}
 * for direct user searches and by {@link com.myoffgridai.memory.service.RagService}
 * for injecting knowledge context into the RAG pipeline.</p>
 */
@Service
public class SemanticSearchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticSearchService.class);

    private final VectorDocumentRepository vectorDocumentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final EmbeddingService embeddingService;

    /**
     * Constructs the semantic search service.
     *
     * @param vectorDocumentRepository the vector document repository
     * @param chunkRepository          the knowledge chunk repository
     * @param documentRepository       the knowledge document repository
     * @param embeddingService         the embedding service
     */
    public SemanticSearchService(VectorDocumentRepository vectorDocumentRepository,
                                  KnowledgeChunkRepository chunkRepository,
                                  KnowledgeDocumentRepository documentRepository,
                                  EmbeddingService embeddingService) {
        this.vectorDocumentRepository = vectorDocumentRepository;
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.embeddingService = embeddingService;
    }

    /**
     * Searches the user's knowledge base for the most semantically similar chunks.
     * Generates the embedding internally — use the overload accepting a pre-computed
     * embedding to avoid redundant embed calls.
     *
     * @param userId    the user's ID
     * @param queryText the search query
     * @param topK      the number of results to return
     * @return a list of search result DTOs with similarity scores
     */
    @Transactional(readOnly = true)
    public List<KnowledgeSearchResultDto> search(UUID userId, String queryText, int topK) {
        float[] queryEmbedding = embeddingService.embed(queryText);
        return search(userId, queryText, topK, queryEmbedding);
    }

    /**
     * Searches the user's knowledge base using a pre-computed embedding vector.
     * Avoids redundant embedding calls when the caller has already embedded the query.
     *
     * @param userId              the user's ID
     * @param queryText           the search query (used for logging)
     * @param topK                the number of results to return
     * @param precomputedEmbedding the pre-computed embedding vector
     * @return a list of search result DTOs with similarity scores
     */
    @Transactional(readOnly = true)
    public List<KnowledgeSearchResultDto> search(UUID userId, String queryText, int topK,
                                                  float[] precomputedEmbedding) {
        log.debug("Semantic search for user {}: '{}' (topK={})", userId, queryText, topK);

        String formattedEmbedding = EmbeddingService.formatEmbedding(precomputedEmbedding);
        List<VectorDocument> vectorDocs = vectorDocumentRepository.findMostSimilarIncludingShared(
                userId, VectorSourceType.KNOWLEDGE_CHUNK.name(),
                formattedEmbedding, topK);

        List<KnowledgeSearchResultDto> results = new ArrayList<>();

        for (VectorDocument vd : vectorDocs) {
            UUID chunkId = vd.getSourceId();
            if (chunkId == null) continue;

            Optional<KnowledgeChunk> chunkOpt = chunkRepository.findById(chunkId);
            if (chunkOpt.isEmpty()) continue;

            KnowledgeChunk chunk = chunkOpt.get();
            KnowledgeDocument doc = chunk.getDocument();

            float similarity = embeddingService.cosineSimilarity(precomputedEmbedding, vd.getEmbedding());

            String documentName = doc.getDisplayName() != null
                    ? doc.getDisplayName() : doc.getFilename();

            results.add(new KnowledgeSearchResultDto(
                    chunk.getId(),
                    doc.getId(),
                    documentName,
                    chunk.getContent(),
                    chunk.getPageNumber(),
                    chunk.getChunkIndex(),
                    similarity
            ));
        }

        log.debug("Semantic search returned {} results", results.size());
        return results;
    }

    /**
     * Retrieves knowledge snippets for RAG context injection, including
     * source document attribution. Generates the embedding internally.
     *
     * @param userId    the user's ID
     * @param queryText the query text to search for
     * @param topK      the number of results to return
     * @return a list of formatted knowledge snippets with source attribution
     */
    @Transactional(readOnly = true)
    public List<String> searchForRagContext(UUID userId, String queryText, int topK) {
        return searchForRagContext(userId, queryText, topK, embeddingService.embed(queryText));
    }

    /**
     * Retrieves knowledge snippets for RAG context injection using a pre-computed
     * embedding vector. Avoids redundant embedding calls when the caller has
     * already embedded the query.
     *
     * @param userId              the user's ID
     * @param queryText           the query text to search for
     * @param topK                the number of results to return
     * @param precomputedEmbedding the pre-computed embedding vector
     * @return a list of formatted knowledge snippets with source attribution
     */
    @Transactional(readOnly = true)
    public List<String> searchForRagContext(UUID userId, String queryText, int topK,
                                            float[] precomputedEmbedding) {
        List<KnowledgeSearchResultDto> results = search(userId, queryText, topK, precomputedEmbedding);
        List<String> snippets = new ArrayList<>();

        for (KnowledgeSearchResultDto result : results) {
            String attributed = "[" + result.documentName() + "]: " + result.content();
            snippets.add(attributed);
        }

        return snippets;
    }
}
