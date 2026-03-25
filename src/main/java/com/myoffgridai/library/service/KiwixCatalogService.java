package com.myoffgridai.library.service;

import com.myoffgridai.library.config.KiwixProperties;
import com.myoffgridai.library.dto.KiwixCatalogEntryDto;
import com.myoffgridai.library.dto.KiwixCatalogSearchResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import reactor.netty.http.client.HttpClient;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxies the Kiwix online catalog (OPDS feed at library.kiwix.org)
 * for browsing and searching available ZIM files.
 *
 * <p>Follows the {@code GutenbergService} caching pattern: browse results
 * are cached in memory with a 1-hour TTL and stale-while-error fallback.
 * Search results are not cached.</p>
 */
@Service
public class KiwixCatalogService {

    private static final Logger log = LoggerFactory.getLogger(KiwixCatalogService.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration BROWSE_CACHE_TTL = Duration.ofHours(1);
    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(20);

    private final WebClient webClient;
    private final KiwixProperties kiwixProperties;
    private final ConcurrentHashMap<String, CachedBrowseResult> browseCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CachedSearchResult> searchCache = new ConcurrentHashMap<>();

    /**
     * Cached browse result with timestamp for TTL expiry.
     */
    private record CachedBrowseResult(KiwixCatalogSearchResultDto result, Instant cachedAt) {
        boolean isFresh() {
            return Instant.now().isBefore(cachedAt.plus(BROWSE_CACHE_TTL));
        }
    }

    /**
     * Cached search result with timestamp for TTL expiry.
     */
    private record CachedSearchResult(KiwixCatalogSearchResultDto result, Instant cachedAt) {
        boolean isFresh() {
            return Instant.now().isBefore(cachedAt.plus(SEARCH_CACHE_TTL));
        }
    }

    /**
     * Constructs the Kiwix catalog service.
     *
     * @param kiwixProperties the kiwix configuration properties
     */
    @Autowired
    public KiwixCatalogService(KiwixProperties kiwixProperties) {
        HttpClient httpClient = HttpClient.create().followRedirect(true);
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(kiwixProperties.getCatalogBaseUrl())
                .defaultHeader("User-Agent", "MyOffGridAI/1.0")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                .build();
        this.kiwixProperties = kiwixProperties;
    }

    /**
     * Test-only constructor allowing injection of a pre-built WebClient.
     */
    KiwixCatalogService(WebClient webClient, KiwixProperties kiwixProperties) {
        this.webClient = webClient;
        this.kiwixProperties = kiwixProperties;
    }

    /**
     * Browses the Kiwix catalog with optional language and category filters.
     *
     * <p>Results are cached in memory for one hour. If the cache is expired and
     * the catalog API is unreachable, stale cached data is returned.</p>
     *
     * @param lang     the language filter (e.g., "eng"), or null for all
     * @param category the category filter (e.g., "wikipedia"), or null for all
     * @param count    the number of entries per page
     * @param start    the offset for pagination
     * @return the catalog search result DTO
     */
    public KiwixCatalogSearchResultDto browse(String lang, String category, int count, int start) {
        String cacheKey = (lang != null ? lang : "") + ":" + (category != null ? category : "") + ":" + count + ":" + start;
        CachedBrowseResult cached = browseCache.get(cacheKey);

        if (cached != null && cached.isFresh()) {
            log.debug("Kiwix catalog browse cache hit for key '{}'", cacheKey);
            return cached.result();
        }

        try {
            String xml = webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/catalog/v2/entries")
                                .queryParam("count", count)
                                .queryParam("start", start);
                        if (lang != null && !lang.isBlank()) {
                            builder.queryParam("lang", lang);
                        }
                        if (category != null && !category.isBlank()) {
                            builder.queryParam("category", category);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(REQUEST_TIMEOUT);

            KiwixCatalogSearchResultDto result = parseAtomFeed(xml);
            browseCache.put(cacheKey, new CachedBrowseResult(result, Instant.now()));
            log.debug("Kiwix catalog browse cache updated for key '{}'", cacheKey);
            return result;
        } catch (Exception e) {
            if (cached != null) {
                log.warn("Kiwix catalog browse failed, serving stale cache: {}", e.getMessage());
                return cached.result();
            }
            log.error("Kiwix catalog browse failed, no cache available: {}", e.getMessage());
            throw new RuntimeException("Kiwix catalog browse unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * Searches the Kiwix catalog by query string.
     *
     * <p>Results are cached in memory for 20 minutes. If the cache is expired and
     * the catalog API is unreachable, stale cached data is returned.</p>
     *
     * @param query the search query
     * @param lang  the language filter, or null for all
     * @param count the number of results
     * @return the catalog search result DTO
     */
    public KiwixCatalogSearchResultDto search(String query, String lang, int count) {
        String cacheKey = query + ":" + (lang != null ? lang : "") + ":" + count;
        CachedSearchResult cached = searchCache.get(cacheKey);

        if (cached != null && cached.isFresh()) {
            log.debug("Kiwix catalog search cache hit for key '{}'", cacheKey);
            return cached.result();
        }

        try {
            String xml = webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/catalog/v2/entries")
                                .queryParam("q", query)
                                .queryParam("count", count);
                        if (lang != null && !lang.isBlank()) {
                            builder.queryParam("lang", lang);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(REQUEST_TIMEOUT);

            KiwixCatalogSearchResultDto result = parseAtomFeed(xml);
            searchCache.put(cacheKey, new CachedSearchResult(result, Instant.now()));
            log.debug("Kiwix catalog search cache updated for key '{}'", cacheKey);
            return result;
        } catch (Exception e) {
            if (cached != null) {
                log.warn("Kiwix catalog search failed for query '{}', serving stale cache: {}",
                        query, e.getMessage());
                return cached.result();
            }
            log.error("Kiwix catalog search failed for query '{}': {}", query, e.getMessage());
            throw new RuntimeException("Kiwix catalog search unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * Parses an Atom XML feed from the Kiwix OPDS catalog into DTOs.
     */
    private KiwixCatalogSearchResultDto parseAtomFeed(String xml) {
        if (xml == null || xml.isBlank()) {
            return new KiwixCatalogSearchResultDto(0, List.of());
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            // Extract total count from opensearch:totalResults
            int totalCount = 0;
            NodeList totalResultsNodes = doc.getElementsByTagNameNS("*", "totalResults");
            if (totalResultsNodes.getLength() > 0) {
                try {
                    totalCount = Integer.parseInt(totalResultsNodes.item(0).getTextContent().trim());
                } catch (NumberFormatException ignored) {
                    // Fall back to counting entries
                }
            }

            NodeList entries = doc.getElementsByTagNameNS("*", "entry");
            List<KiwixCatalogEntryDto> entryDtos = new ArrayList<>();

            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                entryDtos.add(mapEntry(entry));
            }

            if (totalCount == 0) {
                totalCount = entryDtos.size();
            }

            return new KiwixCatalogSearchResultDto(totalCount, entryDtos);
        } catch (Exception e) {
            log.error("Failed to parse Kiwix OPDS feed: {}", e.getMessage());
            return new KiwixCatalogSearchResultDto(0, List.of());
        }
    }

    /**
     * Maps a single Atom {@code <entry>} element to a DTO.
     */
    private KiwixCatalogEntryDto mapEntry(Element entry) {
        String id = getTextContent(entry, "id");
        String title = getTextContent(entry, "title");
        String summary = getTextContent(entry, "summary");
        String language = getTextContent(entry, "language");
        String name = getTextContent(entry, "name");
        String category = getAttributeFromCategory(entry);
        String tags = getTextContent(entry, "tags");

        long articleCount = parseLong(getTextContent(entry, "articleCount"));
        long mediaCount = parseLong(getTextContent(entry, "mediaCount"));

        // Find download URL from <link> with type="application/x-zim"
        String downloadUrl = null;
        long sizeBytes = 0;
        NodeList links = entry.getElementsByTagNameNS("*", "link");
        for (int j = 0; j < links.getLength(); j++) {
            Element link = (Element) links.item(j);
            String type = link.getAttribute("type");
            if ("application/x-zim".equals(type)) {
                downloadUrl = link.getAttribute("href");
                // Strip .meta4 suffix for direct URL
                if (downloadUrl != null && downloadUrl.endsWith(".meta4")) {
                    downloadUrl = downloadUrl.substring(0, downloadUrl.length() - 6);
                }
                String lengthAttr = link.getAttribute("length");
                sizeBytes = parseLong(lengthAttr);
                break;
            }
        }

        // Build illustration URL
        String illustrationUrl = null;
        String uuid = extractUuid(id);
        if (uuid != null) {
            illustrationUrl = kiwixProperties.getCatalogBaseUrl()
                    + "/catalog/v2/illustration/" + uuid + "/?size=48";
        }

        return new KiwixCatalogEntryDto(
                id, title, summary, language, name, category, tags,
                articleCount, mediaCount, sizeBytes, downloadUrl, illustrationUrl
        );
    }

    private String getTextContent(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    private String getAttributeFromCategory(Element entry) {
        NodeList categories = entry.getElementsByTagNameNS("*", "category");
        if (categories.getLength() > 0) {
            return ((Element) categories.item(0)).getAttribute("term");
        }
        return null;
    }

    private String extractUuid(String id) {
        if (id == null) return null;
        // ID is typically a URN like "urn:uuid:abc-123"
        if (id.startsWith("urn:uuid:")) {
            return id.substring("urn:uuid:".length());
        }
        return id;
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
