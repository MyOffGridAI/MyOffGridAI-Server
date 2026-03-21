package com.myoffgridai.enrichment.service;

import com.myoffgridai.enrichment.dto.FetchResult;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.model.DocumentStatus;
import com.myoffgridai.knowledge.service.KnowledgeService;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link WebFetchService}.
 */
@ExtendWith(MockitoExtension.class)
class WebFetchServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private ExternalApiSettingsService settingsService;

    @Mock
    private ClaudeApiService claudeApiService;

    @Mock
    private KnowledgeService knowledgeService;

    private WebFetchService webFetchService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        webFetchService = new WebFetchService(webClientBuilder, settingsService, claudeApiService, knowledgeService);
    }

    @SuppressWarnings("unchecked")
    private void setupWebClientGet(String responseBody) {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(responseBody));
    }

    @Test
    void fetchUrl_extractsTextFromHtml() {
        when(settingsService.getMaxWebFetchSizeKb()).thenReturn(512);

        String html = "<!DOCTYPE html><html><head><title>Test Page</title></head>"
                + "<body><nav>Menu</nav><p>Hello World</p><footer>Footer</footer></body></html>";
        setupWebClientGet(html);

        FetchResult result = webFetchService.fetchUrl("https://example.com/test");

        assertEquals("Test Page", result.title());
        assertTrue(result.content().contains("Hello World"));
        assertFalse(result.content().contains("Menu"));
        assertFalse(result.content().contains("Footer"));
        assertEquals("text/html", result.contentType());
        assertFalse(result.summarized());
    }

    @Test
    void fetchUrl_handlesPlainText() {
        when(settingsService.getMaxWebFetchSizeKb()).thenReturn(512);

        setupWebClientGet("Just plain text content here.");

        FetchResult result = webFetchService.fetchUrl("https://example.com/file.txt");

        assertEquals("Just plain text content here.", result.content());
        assertEquals("text/plain", result.contentType());
    }

    @Test
    void fetchUrl_respectsSizeLimit() {
        when(settingsService.getMaxWebFetchSizeKb()).thenReturn(1); // 1 KB

        String largeContent = "A".repeat(5000);
        setupWebClientGet(largeContent);

        FetchResult result = webFetchService.fetchUrl("https://example.com/large");

        assertTrue(result.content().length() <= 1024);
    }

    @Test
    void fetchUrl_throwsOnNetworkError() {
        when(settingsService.getMaxWebFetchSizeKb()).thenReturn(512);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("Network error")));

        assertThrows(IllegalArgumentException.class,
                () -> webFetchService.fetchUrl("https://example.com/fail"));
    }

    @Test
    void fetchAndStore_createsKnowledgeDocument() {
        when(settingsService.getMaxWebFetchSizeKb()).thenReturn(512);

        String html = "<html><head><title>Article Title</title></head>"
                + "<body><p>Article content here</p></body></html>";
        setupWebClientGet(html);

        KnowledgeDocumentDto expectedDoc = new KnowledgeDocumentDto(
                UUID.randomUUID(), "article_title.txt", "Article Title",
                "application/x-quill-delta", 100L, DocumentStatus.PENDING,
                null, 0, Instant.now(), null, true, true,
                false, true, null
        );
        when(knowledgeService.createFromEditor(any(), any(), any())).thenReturn(expectedDoc);

        UUID userId = UUID.randomUUID();
        KnowledgeDocumentDto result = webFetchService.fetchAndStore(
                "https://example.com/article", userId, false);

        assertNotNull(result);
        assertEquals("Article Title", result.displayName());
        verify(knowledgeService).createFromEditor(eq(userId), eq("Article Title"), any());
    }

    @Test
    void fetchAndStore_summarizesWhenRequested() {
        when(settingsService.getMaxWebFetchSizeKb()).thenReturn(512);
        when(claudeApiService.isAvailable()).thenReturn(true);
        when(claudeApiService.summarizeForKnowledgeBase(any(), anyInt())).thenReturn("Summary text");

        setupWebClientGet("<html><head><title>Page</title></head><body><p>Long content</p></body></html>");

        KnowledgeDocumentDto expectedDoc = new KnowledgeDocumentDto(
                UUID.randomUUID(), "page.txt", "Page",
                "application/x-quill-delta", 50L, DocumentStatus.PENDING,
                null, 0, Instant.now(), null, true, true,
                false, true, null
        );
        when(knowledgeService.createFromEditor(any(), any(), any())).thenReturn(expectedDoc);

        UUID userId = UUID.randomUUID();
        webFetchService.fetchAndStore("https://example.com/page", userId, true);

        verify(claudeApiService).summarizeForKnowledgeBase(any(), anyInt());
    }
}
