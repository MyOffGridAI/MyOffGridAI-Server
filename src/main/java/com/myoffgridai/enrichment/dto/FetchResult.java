package com.myoffgridai.enrichment.dto;

import java.time.Instant;

/**
 * Result of fetching and extracting content from a URL.
 *
 * @param url              the fetched URL
 * @param title            the page title (from HTML title tag or URL)
 * @param content          the extracted readable text
 * @param contentType      the original HTTP content-type
 * @param contentSizeBytes the size of extracted content in bytes
 * @param summarized       whether the content was summarized by Claude
 * @param fetchedAt        when the fetch occurred
 */
public record FetchResult(
        String url,
        String title,
        String content,
        String contentType,
        int contentSizeBytes,
        boolean summarized,
        Instant fetchedAt
) {
}
