package com.myoffgridai.knowledge.dto;

import java.util.List;

/**
 * Internal result record produced by {@link com.myoffgridai.knowledge.service.IngestionService}
 * or {@link com.myoffgridai.knowledge.service.OcrService} after text extraction.
 *
 * @param pages the extracted pages of content
 * @param fullText the concatenated full text from all pages
 */
public record ExtractionResult(List<PageContent> pages, String fullText) {
}
