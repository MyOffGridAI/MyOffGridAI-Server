package com.myoffgridai.memory.service;

import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.common.exception.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sole entry point for generating embeddings via the Ollama embedding model.
 *
 * <p>All other services must call embedding through this class — never call
 * {@link OllamaService#embed(String)} directly from any other class.</p>
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final OllamaService ollamaService;

    /**
     * Constructs the embedding service.
     *
     * @param ollamaService the Ollama integration service
     */
    public EmbeddingService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    /**
     * Generates an embedding vector for the given text.
     *
     * @param text the text to embed
     * @return the raw embedding float array
     * @throws EmbeddingException if OllamaService is unavailable
     */
    public float[] embed(String text) {
        log.debug("Generating embedding for text of length: {}", text.length());
        try {
            return ollamaService.embed(text);
        } catch (Exception e) {
            log.warn("Embedding generation failed: {}", e.getMessage());
            throw new EmbeddingException("Failed to generate embedding", e);
        }
    }

    /**
     * Generates an embedding and formats it as a pgvector string.
     *
     * <p>Returns the format {@code [0.1,0.2,0.3,...]} required for native
     * SQL queries with pgvector's {@code CAST(:embedding AS vector)}.</p>
     *
     * @param text the text to embed
     * @return the embedding in pgvector string format
     * @throws EmbeddingException if OllamaService is unavailable
     */
    public String embedAndFormat(String text) {
        float[] embedding = embed(text);
        return formatEmbedding(embedding);
    }

    /**
     * Generates embeddings for a batch of texts sequentially.
     *
     * @param texts the list of texts to embed
     * @return a list of embedding vectors in the same order as input
     * @throws EmbeddingException if OllamaService is unavailable
     */
    public List<float[]> embedBatch(List<String> texts) {
        log.debug("Generating batch embeddings for {} texts", texts.size());
        try {
            return ollamaService.embedBatch(texts);
        } catch (Exception e) {
            log.warn("Batch embedding generation failed: {}", e.getMessage());
            throw new EmbeddingException("Failed to generate batch embeddings", e);
        }
    }

    /**
     * Computes the cosine similarity between two vectors.
     *
     * @param a first vector
     * @param b second vector
     * @return cosine similarity between 0.0 and 1.0
     */
    public float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0.0f;
        }
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        float denominator = (float) (Math.sqrt(normA) * Math.sqrt(normB));
        if (denominator == 0.0f) {
            return 0.0f;
        }
        return dotProduct / denominator;
    }

    /**
     * Formats a float array as a pgvector string {@code [x,y,z,...]}.
     *
     * @param embedding the embedding array
     * @return the pgvector-compatible string
     */
    public static String formatEmbedding(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
