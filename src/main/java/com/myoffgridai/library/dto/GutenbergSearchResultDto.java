package com.myoffgridai.library.dto;

import java.util.List;

/**
 * Data transfer object for paginated Gutendex API search results.
 *
 * @param count    the total number of matching books
 * @param next     the URL for the next page of results (null if last page)
 * @param previous the URL for the previous page of results (null if first page)
 * @param results  the list of books on this page
 */
public record GutenbergSearchResultDto(
        int count,
        String next,
        String previous,
        List<GutenbergBookDto> results
) {
}
