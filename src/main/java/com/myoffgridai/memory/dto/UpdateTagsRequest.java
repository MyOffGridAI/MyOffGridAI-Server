package com.myoffgridai.memory.dto;

/**
 * Request body for updating the tags associated with a memory.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record UpdateTagsRequest(
        String tags
) {}
