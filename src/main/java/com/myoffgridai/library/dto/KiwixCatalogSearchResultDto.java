package com.myoffgridai.library.dto;

import java.util.List;

/**
 * Paginated result from a Kiwix catalog browse or search query.
 *
 * @param totalCount the total number of matching entries
 * @param entries    the list of catalog entries for the current page
 */
public record KiwixCatalogSearchResultDto(
        int totalCount,
        List<KiwixCatalogEntryDto> entries
) {
}
