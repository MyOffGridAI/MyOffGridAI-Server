package com.myoffgridai.models.dto;

import java.util.List;

/**
 * Search result from the HuggingFace model hub API.
 *
 * @param models     the matching models
 * @param totalCount the total number of matching models
 */
public record HfSearchResultDto(
        List<HfModelDto> models,
        int totalCount
) {
}
