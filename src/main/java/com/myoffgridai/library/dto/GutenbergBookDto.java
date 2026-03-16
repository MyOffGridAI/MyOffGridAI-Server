package com.myoffgridai.library.dto;

import java.util.List;
import java.util.Map;

/**
 * Data transfer object representing a book from the Gutendex (Project Gutenberg) API.
 *
 * @param id            the Gutenberg book ID
 * @param title         the book title
 * @param authors       the list of author names
 * @param subjects      the list of subject categories
 * @param languages     the list of language codes
 * @param downloadCount the total download count from Gutenberg
 * @param formats       a map of MIME type to download URL
 */
public record GutenbergBookDto(
        int id,
        String title,
        List<String> authors,
        List<String> subjects,
        List<String> languages,
        int downloadCount,
        Map<String, String> formats
) {
}
