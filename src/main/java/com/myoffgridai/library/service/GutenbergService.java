package com.myoffgridai.library.service;

import com.myoffgridai.library.config.LibraryProperties;
import com.myoffgridai.library.dto.EbookDto;
import org.springframework.beans.factory.annotation.Autowired;
import com.myoffgridai.library.dto.GutenbergBookDto;
import com.myoffgridai.library.dto.GutenbergSearchResultDto;
import com.myoffgridai.library.model.Ebook;
import com.myoffgridai.library.model.EbookFormat;
import com.myoffgridai.library.repository.EbookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for integrating with the Gutendex API (Project Gutenberg catalog).
 *
 * <p>Supports searching the Gutenberg catalog, fetching book metadata,
 * and importing books into the local eBook library.</p>
 */
@Service
public class GutenbergService {

    private static final Logger log = LoggerFactory.getLogger(GutenbergService.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration BROWSE_CACHE_TTL = Duration.ofHours(1);

    private final WebClient webClient;
    private final EbookRepository ebookRepository;
    private final LibraryProperties libraryProperties;
    private final ConcurrentHashMap<String, CachedBrowseResult> browseCache = new ConcurrentHashMap<>();

    /**
     * Cached browse result with timestamp for TTL expiry.
     */
    private record CachedBrowseResult(GutenbergSearchResultDto result, Instant cachedAt) {
        boolean isFresh() {
            return Instant.now().isBefore(cachedAt.plus(BROWSE_CACHE_TTL));
        }
    }

    /**
     * Constructs the Gutenberg service.
     *
     * <p>Uses a standalone {@code WebClient.builder()} rather than Spring's
     * auto-configured builder to ensure the Reactor Netty {@code followRedirect(true)}
     * setting is not overridden by Spring Boot customizers.</p>
     *
     * @param ebookRepository   the eBook repository
     * @param libraryProperties the library configuration properties
     */
    @Autowired
    public GutenbergService(EbookRepository ebookRepository,
                            LibraryProperties libraryProperties) {
        HttpClient httpClient = HttpClient.create().followRedirect(true);
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(libraryProperties.getGutenbergApiUrl())
                .build();
        this.ebookRepository = ebookRepository;
        this.libraryProperties = libraryProperties;
    }

    /**
     * Test-only constructor allowing injection of a pre-built WebClient.
     */
    GutenbergService(WebClient webClient,
                     EbookRepository ebookRepository,
                     LibraryProperties libraryProperties) {
        this.webClient = webClient;
        this.ebookRepository = ebookRepository;
        this.libraryProperties = libraryProperties;
    }

    /**
     * Browses the Gutendex catalog without a search query.
     *
     * <p>Results are cached in memory for one hour. If the cache is expired and
     * the Gutendex API is slow or unreachable, stale cached data is returned
     * rather than failing. Only the first request (cold cache) blocks on the
     * external API.</p>
     *
     * <p>Valid sort values: {@code "popular"} (default, most downloaded),
     * {@code "ascending"} (lowest ID first), {@code "descending"}
     * (highest ID first, ≈ newest).</p>
     *
     * @param sort  the sort order (popular, ascending, or descending)
     * @param limit the maximum number of results (page size)
     * @return the browse result DTO
     * @throws RuntimeException if the Gutendex API is unreachable and no cached data exists
     */
    public GutenbergSearchResultDto browse(String sort, int limit) {
        String cacheKey = sort + ":" + limit;
        CachedBrowseResult cached = browseCache.get(cacheKey);

        if (cached != null && cached.isFresh()) {
            log.debug("Gutenberg browse cache hit for key '{}'", cacheKey);
            return cached.result();
        }

        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/books")
                            .queryParam("sort", sort)
                            .queryParam("page_size", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(REQUEST_TIMEOUT);

            if (response == null) {
                return new GutenbergSearchResultDto(0, null, null, List.of());
            }

            GutenbergSearchResultDto result = mapSearchResponse(response);
            browseCache.put(cacheKey, new CachedBrowseResult(result, Instant.now()));
            log.debug("Gutenberg browse cache updated for key '{}'", cacheKey);
            return result;
        } catch (Exception e) {
            if (cached != null) {
                log.warn("Gutendex API browse failed (sort='{}'), serving stale cache: {}",
                        sort, e.getMessage());
                return cached.result();
            }
            log.error("Gutendex API browse failed (sort='{}'), no cache available: {}",
                    sort, e.getMessage());
            throw new RuntimeException("Project Gutenberg browse unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * Searches the Gutendex API for books matching the query.
     *
     * @param query the search query
     * @param limit the maximum number of results (page size)
     * @return the search result DTO, or an empty result if the API is unreachable
     */
    public GutenbergSearchResultDto search(String query, int limit) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/books")
                            .queryParam("search", query)
                            .queryParam("page_size", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(REQUEST_TIMEOUT);

            if (response == null) {
                return new GutenbergSearchResultDto(0, null, null, List.of());
            }

            return mapSearchResponse(response);
        } catch (Exception e) {
            log.error("Gutendex API search failed for query '{}': {}", query, e.getMessage());
            throw new RuntimeException("Project Gutenberg search unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches metadata for a single book from the Gutendex API.
     *
     * @param id the Gutenberg book ID
     * @return the book metadata DTO
     * @throws RuntimeException if the API call fails
     */
    public GutenbergBookDto getBookMetadata(int id) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/books/" + id)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(REQUEST_TIMEOUT);

            if (response == null) {
                throw new RuntimeException("No response from Gutendex for book ID " + id);
            }

            return mapBook(response);
        } catch (Exception e) {
            log.error("Gutendex API metadata fetch failed for book {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to fetch Gutenberg book metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Imports a book from Project Gutenberg into the local eBook library.
     *
     * <p>Prefers EPUB format, falls back to plain text. Downloads the file,
     * saves it locally, and persists an Ebook entity with the Gutenberg ID set.</p>
     *
     * @param gutenbergId the Gutenberg book ID
     * @param importedBy  the ID of the importing user
     * @return the imported eBook DTO
     * @throws IllegalArgumentException if already imported
     * @throws RuntimeException         if download or save fails
     */
    @Transactional
    public EbookDto importBook(int gutenbergId, UUID importedBy) {
        String gutenbergIdStr = String.valueOf(gutenbergId);

        if (ebookRepository.existsByGutenbergId(gutenbergIdStr)) {
            throw new IllegalArgumentException("Book with Gutenberg ID " + gutenbergId + " is already imported");
        }

        GutenbergBookDto metadata = getBookMetadata(gutenbergId);

        // Prefer EPUB, fallback to plain text
        String downloadUrl = null;
        EbookFormat format = null;
        String fileExtension = null;

        if (metadata.formats() != null) {
            if (metadata.formats().containsKey("application/epub+zip")) {
                downloadUrl = metadata.formats().get("application/epub+zip");
                format = EbookFormat.EPUB;
                fileExtension = "epub";
            } else if (metadata.formats().containsKey("text/plain; charset=us-ascii")) {
                downloadUrl = metadata.formats().get("text/plain; charset=us-ascii");
                format = EbookFormat.TXT;
                fileExtension = "txt";
            } else if (metadata.formats().containsKey("text/plain")) {
                downloadUrl = metadata.formats().get("text/plain");
                format = EbookFormat.TXT;
                fileExtension = "txt";
            }
        }

        if (downloadUrl == null) {
            throw new RuntimeException("No downloadable format found for Gutenberg book " + gutenbergId);
        }

        try {
            HttpClient downloadHttpClient = HttpClient.create().followRedirect(true);
            byte[] fileBytes = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(downloadHttpClient))
                    .build()
                    .get()
                    .uri(downloadUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(Duration.ofSeconds(60));

            if (fileBytes == null || fileBytes.length == 0) {
                throw new RuntimeException("Empty response downloading Gutenberg book " + gutenbergId);
            }

            String slug = metadata.title().toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-|-$", "");
            if (slug.length() > 50) {
                slug = slug.substring(0, 50);
            }
            String filename = String.format("gutenberg-%d-%s.%s", gutenbergId, slug, fileExtension);

            Path ebookDir = Paths.get(libraryProperties.getEbookDirectory());
            Files.createDirectories(ebookDir);
            Path targetPath = ebookDir.resolve(filename);
            Files.write(targetPath, fileBytes);

            String authorStr = metadata.authors() != null && !metadata.authors().isEmpty()
                    ? String.join(", ", metadata.authors())
                    : null;

            Ebook ebook = new Ebook();
            ebook.setTitle(metadata.title());
            ebook.setAuthor(authorStr);
            ebook.setLanguage(metadata.languages() != null && !metadata.languages().isEmpty()
                    ? metadata.languages().getFirst() : null);
            ebook.setFormat(format);
            ebook.setFileSizeBytes(fileBytes.length);
            ebook.setFilePath(targetPath.toString());
            ebook.setGutenbergId(gutenbergIdStr);
            ebook.setUploadedBy(importedBy);

            Ebook saved = ebookRepository.save(ebook);
            log.info("Imported Gutenberg book {}: '{}' as {} ({} bytes)",
                    gutenbergId, metadata.title(), format, fileBytes.length);
            return EbookDto.from(saved);

        } catch (IOException e) {
            log.error("Failed to save Gutenberg book {} to disk: {}", gutenbergId, e.getMessage(), e);
            throw new RuntimeException("Failed to save imported book: " + e.getMessage(), e);
        }
    }

    /**
     * Maps the raw Gutendex API search response to a typed DTO.
     */
    @SuppressWarnings("unchecked")
    private GutenbergSearchResultDto mapSearchResponse(Map<String, Object> response) {
        int count = response.get("count") instanceof Number n ? n.intValue() : 0;
        String next = response.get("next") instanceof String s ? s : null;
        String previous = response.get("previous") instanceof String s ? s : null;

        List<Map<String, Object>> rawResults = response.get("results") instanceof List<?> list
                ? (List<Map<String, Object>>) list : List.of();

        List<GutenbergBookDto> results = rawResults.stream()
                .map(this::mapBook)
                .toList();

        return new GutenbergSearchResultDto(count, next, previous, results);
    }

    /**
     * Maps a raw Gutendex book JSON object to a typed DTO.
     */
    @SuppressWarnings("unchecked")
    private GutenbergBookDto mapBook(Map<String, Object> raw) {
        int id = raw.get("id") instanceof Number n ? n.intValue() : 0;
        String title = raw.get("title") instanceof String s ? s : "";

        List<String> authors = new ArrayList<>();
        if (raw.get("authors") instanceof List<?> authorList) {
            for (Object a : authorList) {
                if (a instanceof Map<?, ?> authorMap && authorMap.get("name") instanceof String name) {
                    authors.add(name);
                }
            }
        }

        List<String> subjects = raw.get("subjects") instanceof List<?> list
                ? list.stream().filter(String.class::isInstance).map(String.class::cast).toList()
                : List.of();

        List<String> languages = raw.get("languages") instanceof List<?> list
                ? list.stream().filter(String.class::isInstance).map(String.class::cast).toList()
                : List.of();

        int downloadCount = raw.get("download_count") instanceof Number n ? n.intValue() : 0;

        Map<String, String> formats = new LinkedHashMap<>();
        if (raw.get("formats") instanceof Map<?, ?> fmts) {
            fmts.forEach((k, v) -> {
                if (k instanceof String key && v instanceof String value) {
                    formats.put(key, value);
                }
            });
        }

        return new GutenbergBookDto(id, title, authors, subjects, languages, downloadCount, formats);
    }
}
