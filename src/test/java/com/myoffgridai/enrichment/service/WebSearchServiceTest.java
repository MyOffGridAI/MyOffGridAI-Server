package com.myoffgridai.enrichment.service;

import com.myoffgridai.enrichment.dto.SearchResultDto;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.model.DocumentStatus;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link WebSearchService}.
 */
@ExtendWith(MockitoExtension.class)
class WebSearchServiceTest {

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
    private WebFetchService webFetchService;

    private WebSearchService webSearchService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        webSearchService = new WebSearchService(webClientBuilder, settingsService, webFetchService);
    }

    @Test
    void isAvailable_returnsTrueWhenKeyConfigured() {
        when(settingsService.getBraveKey()).thenReturn(Optional.of("brave-key"));
        assertTrue(webSearchService.isAvailable());
    }

    @Test
    void isAvailable_returnsFalseWhenNoKey() {
        when(settingsService.getBraveKey()).thenReturn(Optional.empty());
        assertFalse(webSearchService.isAvailable());
    }

    @Test
    void search_returnsEmptyWhenNoKey() {
        when(settingsService.getBraveKey()).thenReturn(Optional.empty());

        List<SearchResultDto> results = webSearchService.search("test query");

        assertTrue(results.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void search_returnsResults() {
        when(settingsService.getBraveKey()).thenReturn(Optional.of("brave-key"));
        when(settingsService.getSearchResultLimit()).thenReturn(5);

        Map<String, Object> braveResponse = Map.of(
                "web", Map.of(
                        "results", List.of(
                                Map.of("title", "Result 1", "url", "https://example.com/1",
                                        "description", "Description 1"),
                                Map.of("title", "Result 2", "url", "https://example.com/2",
                                        "description", "Description 2")
                        )
                )
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(), any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(braveResponse));

        List<SearchResultDto> results = webSearchService.search("test");

        assertEquals(2, results.size());
        assertEquals("Result 1", results.get(0).title());
        assertEquals("https://example.com/1", results.get(0).url());
    }

    @SuppressWarnings("unchecked")
    @Test
    void search_returnsEmptyOnApiError() {
        when(settingsService.getBraveKey()).thenReturn(Optional.of("brave-key"));
        when(settingsService.getSearchResultLimit()).thenReturn(5);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(), any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.error(new WebClientResponseException(500, "Server error", null, null, null)));

        List<SearchResultDto> results = webSearchService.search("test");

        assertTrue(results.isEmpty());
    }

    @Test
    void searchAndStore_returnsEmptyWhenNoResults() {
        when(settingsService.getBraveKey()).thenReturn(Optional.empty());

        List<KnowledgeDocumentDto> stored = webSearchService.searchAndStore(
                "test", 3, UUID.randomUUID(), false);

        assertTrue(stored.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void searchAndStore_fetchesAndStoresTopN() {
        when(settingsService.getBraveKey()).thenReturn(Optional.of("brave-key"));
        when(settingsService.getSearchResultLimit()).thenReturn(5);

        Map<String, Object> braveResponse = Map.of(
                "web", Map.of(
                        "results", List.of(
                                Map.of("title", "R1", "url", "https://a.com", "description", "D1"),
                                Map.of("title", "R2", "url", "https://b.com", "description", "D2"),
                                Map.of("title", "R3", "url", "https://c.com", "description", "D3")
                        )
                )
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(), any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(braveResponse));

        KnowledgeDocumentDto doc = new KnowledgeDocumentDto(
                UUID.randomUUID(), "file.txt", "Title",
                "application/x-quill-delta", 100L, DocumentStatus.PENDING,
                null, 0, Instant.now(), null, true, true,
                false, true, null
        );
        when(webFetchService.fetchAndStore(anyString(), any(), anyBoolean())).thenReturn(doc);

        UUID userId = UUID.randomUUID();
        List<KnowledgeDocumentDto> stored = webSearchService.searchAndStore(
                "test", 2, userId, false);

        assertEquals(2, stored.size());
        verify(webFetchService, times(2)).fetchAndStore(anyString(), eq(userId), eq(false));
    }
}
