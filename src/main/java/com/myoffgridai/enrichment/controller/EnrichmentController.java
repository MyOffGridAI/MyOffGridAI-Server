package com.myoffgridai.enrichment.controller;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.enrichment.dto.*;
import com.myoffgridai.enrichment.service.ClaudeApiService;
import com.myoffgridai.enrichment.service.WebFetchService;
import com.myoffgridai.enrichment.service.WebSearchService;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for web enrichment operations — URL fetching, web search,
 * and Knowledge Base ingestion.
 *
 * <p>All endpoints require authentication. Enrichment features degrade
 * gracefully when external API keys are not configured.</p>
 */
@RestController
@RequestMapping("/api/enrichment")
public class EnrichmentController {

    private final WebFetchService webFetchService;
    private final WebSearchService webSearchService;
    private final ClaudeApiService claudeApiService;
    private final ExternalApiSettingsService settingsService;

    /**
     * Constructs the enrichment controller.
     *
     * @param webFetchService  the web fetch service
     * @param webSearchService the web search service
     * @param claudeApiService the Claude API service
     * @param settingsService  the external API settings service
     */
    public EnrichmentController(WebFetchService webFetchService,
                                WebSearchService webSearchService,
                                ClaudeApiService claudeApiService,
                                ExternalApiSettingsService settingsService) {
        this.webFetchService = webFetchService;
        this.webSearchService = webSearchService;
        this.claudeApiService = claudeApiService;
        this.settingsService = settingsService;
    }

    /**
     * Fetches content from a URL and stores it in the Knowledge Base.
     *
     * @param request   the fetch URL request
     * @param principal the authenticated user
     * @return the fetch result and created document
     */
    @PostMapping("/fetch-url")
    public ResponseEntity<ApiResponse<KnowledgeDocumentDto>> fetchUrl(
            @Valid @RequestBody FetchUrlRequest request,
            @AuthenticationPrincipal User principal) {
        KnowledgeDocumentDto doc = webFetchService.fetchAndStore(
                request.url(), principal.getId(), request.summarizeWithClaude());
        return ResponseEntity.ok(ApiResponse.success(doc, "URL content fetched and stored"));
    }

    /**
     * Searches the web and optionally stores top results in the Knowledge Base.
     *
     * @param request   the search request
     * @param principal the authenticated user
     * @return the search results and any stored documents
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<SearchEnrichmentResultDto>> search(
            @Valid @RequestBody SearchRequest request,
            @AuthenticationPrincipal User principal) {
        List<SearchResultDto> results = webSearchService.search(request.query());
        List<KnowledgeDocumentDto> stored = List.of();

        if (request.storeTopN() > 0 && !results.isEmpty()) {
            stored = webSearchService.searchAndStore(
                    request.query(), request.storeTopN(),
                    principal.getId(), request.summarizeWithClaude());
        }

        SearchEnrichmentResultDto result = new SearchEnrichmentResultDto(results, stored);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Returns the current enrichment subsystem status.
     *
     * @return enrichment availability and configuration
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<EnrichmentStatusDto>> getStatus() {
        EnrichmentStatusDto status = new EnrichmentStatusDto(
                claudeApiService.isAvailable(),
                webSearchService.isAvailable(),
                settingsService.getMaxWebFetchSizeKb(),
                settingsService.getSearchResultLimit()
        );
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
