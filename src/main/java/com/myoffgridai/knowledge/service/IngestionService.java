package com.myoffgridai.knowledge.service;

import com.myoffgridai.knowledge.dto.ExtractionResult;
import com.myoffgridai.knowledge.dto.PageContent;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts text from PDF and plain-text/markdown documents.
 *
 * <p>Uses Apache PDFBox for PDF extraction and plain reading for text-based
 * formats. Image-based documents are delegated to {@link OcrService}.</p>
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    /**
     * Extracts text from a PDF document, page by page.
     *
     * @param inputStream the PDF file input stream
     * @return an extraction result containing per-page content
     * @throws IOException if the PDF cannot be read
     */
    public ExtractionResult extractPdf(InputStream inputStream) throws IOException {
        log.debug("Extracting text from PDF");
        List<PageContent> pages = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();

        byte[] bytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();
            log.debug("PDF has {} pages", totalPages);

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document).trim();
                if (!pageText.isEmpty()) {
                    pages.add(new PageContent(i, pageText));
                    fullText.append(pageText).append("\n");
                }
            }
        }

        log.info("Extracted {} pages from PDF", pages.size());
        return new ExtractionResult(pages, fullText.toString().trim());
    }

    /**
     * Extracts text from a plain-text or markdown file.
     *
     * @param inputStream the file input stream
     * @return an extraction result with the full text as a single page
     * @throws IOException if the file cannot be read
     */
    public ExtractionResult extractText(InputStream inputStream) throws IOException {
        log.debug("Extracting text from plain-text/markdown file");
        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        List<PageContent> pages = List.of(new PageContent(null, content));
        return new ExtractionResult(pages, content);
    }
}
