package com.myoffgridai.enrichment.dto;

import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;

import java.util.List;

/**
 * Result of a web search enrichment operation.
 *
 * @param results the search results
 * @param stored  the documents stored in the Knowledge Base (may be empty)
 */
public record SearchEnrichmentResultDto(
        List<SearchResultDto> results,
        List<KnowledgeDocumentDto> stored
) {
}
