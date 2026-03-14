package com.myoffgridai.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for searching the knowledge base.
 *
 * @param query the search query text
 * @param topK  the number of top results to return (defaults to 5 if null)
 */
public record KnowledgeSearchRequest(
        @NotBlank(message = "Query must not be blank")
        @Size(max = 2000, message = "Query must not exceed 2000 characters")
        String query,
        Integer topK
) {
}
