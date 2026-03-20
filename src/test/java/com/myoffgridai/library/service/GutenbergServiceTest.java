package com.myoffgridai.library.service;

import com.myoffgridai.library.config.LibraryProperties;
import com.myoffgridai.library.dto.EbookDto;
import com.myoffgridai.library.dto.GutenbergBookDto;
import com.myoffgridai.library.dto.GutenbergSearchResultDto;
import com.myoffgridai.library.model.Ebook;
import com.myoffgridai.library.model.EbookFormat;
import com.myoffgridai.library.repository.EbookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GutenbergService}.
 */
@ExtendWith(MockitoExtension.class)
class GutenbergServiceTest {

    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;
    @Mock private EbookRepository ebookRepository;
    @Mock private LibraryProperties libraryProperties;

    @TempDir
    Path tempDir;

    private GutenbergService gutenbergService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(libraryProperties.getGutenbergApiUrl()).thenReturn("https://gutendex.com");
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        gutenbergService = new GutenbergService(webClientBuilder, ebookRepository, libraryProperties);
    }

    @Test
    @SuppressWarnings("unchecked")
    void browse_popularSort_returnsResults() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", 2);
        response.put("next", null);
        response.put("previous", null);

        Map<String, Object> book1 = new LinkedHashMap<>();
        book1.put("id", 1342);
        book1.put("title", "Pride and Prejudice");
        book1.put("authors", List.of(Map.of("name", "Austen, Jane")));
        book1.put("subjects", List.of("Fiction"));
        book1.put("languages", List.of("en"));
        book1.put("download_count", 80000);
        book1.put("formats", Map.of("application/epub+zip", "https://gutenberg.org/1342.epub"));

        Map<String, Object> book2 = new LinkedHashMap<>();
        book2.put("id", 84);
        book2.put("title", "Frankenstein");
        book2.put("authors", List.of(Map.of("name", "Shelley, Mary")));
        book2.put("subjects", List.of("Science fiction"));
        book2.put("languages", List.of("en"));
        book2.put("download_count", 100000);
        book2.put("formats", Map.of("application/epub+zip", "https://gutenberg.org/84.epub"));

        response.put("results", List.of(book1, book2));

        WebClient.RequestHeadersUriSpec requestSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));

        GutenbergSearchResultDto result = gutenbergService.browse("popular", 10);

        assertThat(result.count()).isEqualTo(2);
        assertThat(result.results()).hasSize(2);
        assertThat(result.results().getFirst().title()).isEqualTo("Pride and Prejudice");
    }

    @Test
    @SuppressWarnings("unchecked")
    void browse_apiUnavailable_throwsRuntime() {
        WebClient.RequestHeadersUriSpec requestSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        assertThatThrownBy(() -> gutenbergService.browse("popular", 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("browse unavailable");
    }

    @Test
    @SuppressWarnings("unchecked")
    void browse_nullResponse_returnsEmptyResult() {
        WebClient.RequestHeadersUriSpec requestSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.empty());

        GutenbergSearchResultDto result = gutenbergService.browse("descending", 10);

        assertThat(result.count()).isEqualTo(0);
        assertThat(result.results()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_validQuery_returnsResults() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", 1);
        response.put("next", null);
        response.put("previous", null);

        Map<String, Object> book = new LinkedHashMap<>();
        book.put("id", 1342);
        book.put("title", "Pride and Prejudice");
        book.put("authors", List.of(Map.of("name", "Austen, Jane")));
        book.put("subjects", List.of("Fiction"));
        book.put("languages", List.of("en"));
        book.put("download_count", 50000);
        book.put("formats", Map.of("application/epub+zip", "https://gutenberg.org/ebooks/1342.epub.images"));
        response.put("results", List.of(book));

        WebClient.RequestHeadersUriSpec requestSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));

        GutenbergSearchResultDto result = gutenbergService.search("pride", 20);

        assertThat(result.count()).isEqualTo(1);
        assertThat(result.results()).hasSize(1);
        assertThat(result.results().getFirst().title()).isEqualTo("Pride and Prejudice");
        assertThat(result.results().getFirst().authors()).contains("Austen, Jane");
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_apiUnavailable_throwsRuntime() {
        WebClient.RequestHeadersUriSpec requestSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        assertThatThrownBy(() -> gutenbergService.search("test", 20))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("search unavailable");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBookMetadata_validId_returnsDto() {
        Map<String, Object> book = new LinkedHashMap<>();
        book.put("id", 84);
        book.put("title", "Frankenstein");
        book.put("authors", List.of(Map.of("name", "Shelley, Mary")));
        book.put("subjects", List.of("Science fiction"));
        book.put("languages", List.of("en"));
        book.put("download_count", 100000);
        book.put("formats", Map.of("application/epub+zip", "https://gutenberg.org/84.epub"));

        WebClient.RequestHeadersUriSpec requestSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri("/books/84")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(book));

        GutenbergBookDto result = gutenbergService.getBookMetadata(84);

        assertThat(result.title()).isEqualTo("Frankenstein");
        assertThat(result.id()).isEqualTo(84);
    }

    @Test
    void importBook_alreadyImported_throwsIllegalArgument() {
        when(ebookRepository.existsByGutenbergId("1342")).thenReturn(true);

        assertThatThrownBy(() -> gutenbergService.importBook(1342, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already imported");
    }

    @Test
    @SuppressWarnings("unchecked")
    void importBook_epubAvailable_failsDownload_throwsRuntime() {
        when(ebookRepository.existsByGutenbergId("1342")).thenReturn(false);

        // Mock getBookMetadata response
        Map<String, Object> book = new LinkedHashMap<>();
        book.put("id", 1342);
        book.put("title", "Pride and Prejudice");
        book.put("authors", List.of(Map.of("name", "Austen, Jane")));
        book.put("subjects", List.of("Fiction"));
        book.put("languages", List.of("en"));
        book.put("download_count", 50000);
        book.put("formats", Map.of("application/epub+zip", "https://gutenberg.org/1342.epub"));

        WebClient.RequestHeadersUriSpec requestSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri("/books/1342")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(book));

        // The importBook will try to download from the URL using WebClient.create()
        // which will fail in test, so we expect a RuntimeException
        assertThatThrownBy(() -> gutenbergService.importBook(1342, UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void importBook_noFormats_throwsRuntime() {
        when(ebookRepository.existsByGutenbergId("999")).thenReturn(false);

        Map<String, Object> book = new LinkedHashMap<>();
        book.put("id", 999);
        book.put("title", "No Formats");
        book.put("authors", List.of());
        book.put("subjects", List.of());
        book.put("languages", List.of());
        book.put("download_count", 0);
        book.put("formats", Map.of());

        WebClient.RequestHeadersUriSpec requestSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri("/books/999")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(book));

        assertThatThrownBy(() -> gutenbergService.importBook(999, UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No downloadable format");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBookMetadata_apiError_throwsRuntime() {
        WebClient.RequestHeadersUriSpec requestSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri("/books/9999")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Not Found")));

        assertThatThrownBy(() -> gutenbergService.getBookMetadata(9999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch Gutenberg book metadata");
    }
}
