package com.myoffgridai.enrichment.service;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.enrichment.dto.SearchResultDto;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Searches the web using the Brave Search API and optionally ingests
 * results into the Knowledge Base.
 *
 * <p>Requires a Brave Search API key configured in {@link ExternalApiSettingsService}.
 * If no key is configured, returns an empty result set.</p>
 */
@Service
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);

    private final WebClient webClient;
    private final ExternalApiSettingsService settingsService;
    private final WebFetchService webFetchService;

    /**
     * Constructs the web search service.
     *
     * @param webClientBuilder the WebClient builder
     * @param settingsService  the external API settings service
     * @param webFetchService  the web fetch service
     */
    public WebSearchService(WebClient.Builder webClientBuilder,
                            ExternalApiSettingsService settingsService,
                            WebFetchService webFetchService) {
        this.webClient = webClientBuilder.build();
        this.settingsService = settingsService;
        this.webFetchService = webFetchService;
    }

    /**
     * Returns true if the Brave Search API is configured and enabled.
     *
     * @return true if available
     */
    public boolean isAvailable() {
        return settingsService.getBraveKey().isPresent();
    }

    /**
     * Searches the web and returns result summaries. Does not store anything.
     *
     * @param query the search query
     * @return the search results, or empty list if Brave API unavailable
     */
    public List<SearchResultDto> search(String query) {
        Optional<String> keyOpt = settingsService.getBraveKey();
        if (keyOpt.isEmpty()) {
            log.info("Brave Search API not configured");
            return Collections.emptyList();
        }

        int limit = settingsService.getSearchResultLimit();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(AppConstants.BRAVE_SEARCH_API_URL + "?q={query}&count={count}", query, limit)
                    .header("X-Subscription-Token", keyOpt.get())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(AppConstants.WEB_FETCH_TIMEOUT_SECONDS))
                    .block();

            if (response == null) {
                log.warn("Brave Search returned null response");
                return Collections.emptyList();
            }

            return parseSearchResults(response);
        } catch (Exception e) {
            log.warn("Brave Search API call failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Searches, fetches top N results, and stores them in Knowledge Base.
     *
     * @param query               the search query
     * @param fetchTopN           how many top results to fetch and store
     * @param userId              the owning user's ID
     * @param summarizeWithClaude whether to summarize fetched content using Claude
     * @return the documents stored in the Knowledge Base
     */
    public List<KnowledgeDocumentDto> searchAndStore(String query, int fetchTopN,
                                                      UUID userId, boolean summarizeWithClaude) {
        List<SearchResultDto> results = search(query);
        if (results.isEmpty() || fetchTopN <= 0) {
            return Collections.emptyList();
        }

        List<KnowledgeDocumentDto> stored = new ArrayList<>();
        int toFetch = Math.min(fetchTopN, results.size());

        for (int i = 0; i < toFetch; i++) {
            SearchResultDto result = results.get(i);
            try {
                KnowledgeDocumentDto doc = webFetchService.fetchAndStore(
                        result.url(), userId, summarizeWithClaude);
                stored.add(doc);
            } catch (Exception e) {
                log.warn("Failed to fetch and store search result {}: {}", result.url(), e.getMessage());
            }
        }

        log.info("Stored {}/{} search results for query '{}' as knowledge documents",
                stored.size(), toFetch, query);
        return stored;
    }

    @SuppressWarnings("unchecked")
    private List<SearchResultDto> parseSearchResults(Map<String, Object> response) {
        Map<String, Object> web = (Map<String, Object>) response.get("web");
        if (web == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) web.get("results");
        if (results == null) {
            return Collections.emptyList();
        }

        return results.stream()
                .map(r -> new SearchResultDto(
                        (String) r.getOrDefault("title", ""),
                        (String) r.getOrDefault("url", ""),
                        (String) r.getOrDefault("description", ""),
                        (String) r.get("published_date")
                ))
                .toList();
    }
}
