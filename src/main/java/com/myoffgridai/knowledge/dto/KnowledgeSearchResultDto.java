package com.myoffgridai.knowledge.dto;

import java.util.UUID;

/**
 * Response DTO for a knowledge search result, pairing a chunk of content
 * with its similarity score and source document metadata.
 *
 * @param chunkId       the ID of the matching knowledge chunk
 * @param documentId    the ID of the source document
 * @param documentName  the display name or filename of the source document
 * @param content       the chunk text content
 * @param pageNumber    the page number (may be null for non-paginated documents)
 * @param chunkIndex    the index of the chunk within the document
 * @param similarityScore the cosine similarity score (0.0 to 1.0)
 */
public record KnowledgeSearchResultDto(
        UUID chunkId,
        UUID documentId,
        String documentName,
        String content,
        Integer pageNumber,
        int chunkIndex,
        float similarityScore
) {
}
