package com.myoffgridai.knowledge.dto;

/**
 * Represents a single page of extracted text content.
 *
 * @param pageNumber the 1-based page number (null for non-paginated documents)
 * @param content the extracted text for this page
 */
public record PageContent(Integer pageNumber, String content) {
}
