package com.myoffgridai.knowledge.dto;

import com.myoffgridai.knowledge.model.KnowledgeChunk;

/**
 * Internal record pairing a {@link KnowledgeChunk} with its cosine similarity score.
 *
 * @param chunk           the matching knowledge chunk
 * @param similarityScore the cosine similarity score (higher is more similar)
 */
public record SemanticSearchResult(KnowledgeChunk chunk, float similarityScore) {
}
