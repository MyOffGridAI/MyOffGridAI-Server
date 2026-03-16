package com.myoffgridai.enrichment.service;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.enrichment.dto.FetchResult;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.service.KnowledgeService;
import com.myoffgridai.knowledge.util.DeltaJsonUtils;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Fetches and extracts readable text content from web URLs.
 *
 * <p>Uses Jsoup for HTML parsing and text extraction. Respects the
 * {@code maxWebFetchSizeKb} limit configured in {@link ExternalApiSettingsService}.
 * Raw fetched content is optionally summarized via {@link ClaudeApiService}
 * before Knowledge Base ingestion.</p>
 */
@Service
public class WebFetchService {

    private static final Logger log = LoggerFactory.getLogger(WebFetchService.class);

    private final WebClient webClient;
    private final ExternalApiSettingsService settingsService;
    private final ClaudeApiService claudeApiService;
    private final KnowledgeService knowledgeService;

    /**
     * Constructs the web fetch service.
     *
     * @param webClientBuilder the WebClient builder
     * @param settingsService  the external API settings service
     * @param claudeApiService the Claude API service
     * @param knowledgeService the knowledge service
     */
    public WebFetchService(WebClient.Builder webClientBuilder,
                           ExternalApiSettingsService settingsService,
                           ClaudeApiService claudeApiService,
                           KnowledgeService knowledgeService) {
        this.webClient = webClientBuilder.build();
        this.settingsService = settingsService;
        this.claudeApiService = claudeApiService;
        this.knowledgeService = knowledgeService;
    }

    /**
     * Fetches URL content and returns extracted readable text.
     *
     * @param url the URL to fetch
     * @return the fetch result with extracted content
     */
    public FetchResult fetchUrl(String url) {
        int maxSizeKb = settingsService.getMaxWebFetchSizeKb();
        int maxBytes = maxSizeKb * 1024;

        try {
            String rawContent = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(AppConstants.WEB_FETCH_TIMEOUT_SECONDS))
                    .block();

            if (rawContent == null || rawContent.isBlank()) {
                return new FetchResult(url, url, "", "text/html", 0, false, Instant.now());
            }

            // Truncate raw response if too large
            if (rawContent.length() > maxBytes) {
                rawContent = rawContent.substring(0, maxBytes);
            }

            // Extract readable text from HTML
            String title;
            String content;
            String contentType;

            if (looksLikeHtml(rawContent)) {
                Document doc = Jsoup.parse(rawContent);
                doc.select("nav, footer, script, style, iframe, aside, header").remove();
                title = doc.title().isBlank() ? extractTitleFromUrl(url) : doc.title();
                content = doc.body() != null ? doc.body().text() : doc.text();
                contentType = "text/html";
            } else {
                title = extractTitleFromUrl(url);
                content = rawContent;
                contentType = "text/plain";
            }

            // Enforce size limit on extracted content
            if (content.length() > maxBytes) {
                content = content.substring(0, maxBytes);
            }

            return new FetchResult(
                    url, title, content, contentType,
                    content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                    false, Instant.now()
            );
        } catch (Exception e) {
            log.warn("Failed to fetch URL {}: {}", url, e.getMessage());
            throw new IllegalArgumentException("Failed to fetch URL: " + e.getMessage());
        }
    }

    /**
     * Fetches a URL, optionally summarizes content via Claude, and stores
     * the result in the Knowledge Base.
     *
     * @param url                 the URL to fetch
     * @param userId              the owning user's ID
     * @param summarizeWithClaude whether to summarize using Claude
     * @return the created Knowledge Base document DTO
     */
    public KnowledgeDocumentDto fetchAndStore(String url, UUID userId, boolean summarizeWithClaude) {
        FetchResult result = fetchUrl(url);
        String content = result.content();
        boolean summarized = false;

        if (summarizeWithClaude && claudeApiService.isAvailable() && !content.isBlank()) {
            content = claudeApiService.summarizeForKnowledgeBase(content, result.contentSizeBytes());
            summarized = true;
        }

        String deltaJson = DeltaJsonUtils.textToDeltaJson(content);
        KnowledgeDocumentDto doc = knowledgeService.createFromEditor(userId, result.title(), deltaJson);

        log.info("Stored web content from {} as document {} (summarized={})",
                url, doc.id(), summarized);
        return doc;
    }

    private boolean looksLikeHtml(String content) {
        String trimmed = content.trim().toLowerCase();
        return trimmed.startsWith("<!doctype") || trimmed.startsWith("<html") || trimmed.startsWith("<head");
    }

    private String extractTitleFromUrl(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            String path = uri.getPath();
            if (path != null && !path.isBlank() && !path.equals("/")) {
                String lastSegment = path.substring(path.lastIndexOf('/') + 1);
                if (!lastSegment.isBlank()) {
                    return lastSegment.replace('-', ' ').replace('_', ' ');
                }
            }
            return uri.getHost() != null ? uri.getHost() : url;
        } catch (Exception e) {
            return url;
        }
    }
}
