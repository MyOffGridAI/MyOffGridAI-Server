package com.myoffgridai.library.dto;

/**
 * Represents a single entry from the Kiwix online catalog (OPDS feed).
 *
 * @param id              the catalog entry UUID
 * @param title           the content title
 * @param description     the content description/summary
 * @param language        the ISO language code
 * @param name            the short name identifier
 * @param category        the content category (e.g., "wikipedia", "wiktionary")
 * @param tags            semicolon-separated tags
 * @param articleCount    the number of articles
 * @param mediaCount      the number of media items
 * @param sizeBytes       the file size in bytes
 * @param downloadUrl     the direct ZIM download URL
 * @param illustrationUrl the illustration/thumbnail URL
 */
public record KiwixCatalogEntryDto(
        String id,
        String title,
        String description,
        String language,
        String name,
        String category,
        String tags,
        long articleCount,
        long mediaCount,
        long sizeBytes,
        String downloadUrl,
        String illustrationUrl
) {
}
