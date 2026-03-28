package com.myoffgridai.knowledge.service;

import com.myoffgridai.knowledge.dto.ExtractionResult;
import com.myoffgridai.knowledge.dto.PageContent;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;
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
                String pageText = sanitize(stripper.getText(document).trim());
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
        String content = sanitize(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim());
        List<PageContent> pages = List.of(new PageContent(null, content));
        return new ExtractionResult(pages, content);
    }

    /**
     * Extracts text from a DOCX (Office Open XML) document.
     *
     * @param inputStream the DOCX file input stream
     * @return an extraction result with paragraphs as a single page
     * @throws IOException if the document cannot be read
     */
    public ExtractionResult extractDocx(InputStream inputStream) throws IOException {
        log.debug("Extracting text from DOCX");
        StringBuilder fullText = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = sanitize(paragraph.getText());
                if (text != null && !text.isBlank()) {
                    fullText.append(text).append("\n");
                }
            }
        }
        String text = fullText.toString().trim();
        List<PageContent> pages = text.isEmpty()
                ? List.of() : List.of(new PageContent(null, text));
        log.info("Extracted {} characters from DOCX", text.length());
        return new ExtractionResult(pages, text);
    }

    /**
     * Extracts text from a legacy DOC (BIFF) document.
     *
     * @param inputStream the DOC file input stream
     * @return an extraction result with the full text as a single page
     * @throws IOException if the document cannot be read
     */
    public ExtractionResult extractDoc(InputStream inputStream) throws IOException {
        log.debug("Extracting text from DOC");
        String text;
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            text = sanitize(extractor.getText().trim());
        }
        List<PageContent> pages = text.isEmpty()
                ? List.of() : List.of(new PageContent(null, text));
        log.info("Extracted {} characters from DOC", text.length());
        return new ExtractionResult(pages, text);
    }

    /**
     * Extracts text from an RTF (Rich Text Format) document.
     *
     * <p>Uses Java's built-in {@link RTFEditorKit} to parse the RTF stream
     * and extract plain text. The entire content is returned as a single page.</p>
     *
     * @param inputStream the RTF file input stream
     * @return an extraction result with the full text as a single page
     * @throws IOException if the document cannot be read
     */
    public ExtractionResult extractRtf(InputStream inputStream) throws IOException {
        log.debug("Extracting text from RTF");
        RTFEditorKit rtfKit = new RTFEditorKit();
        Document doc = rtfKit.createDefaultDocument();
        try {
            rtfKit.read(inputStream, doc, 0);
            String text = sanitize(doc.getText(0, doc.getLength()).trim());
            List<PageContent> pages = text.isEmpty()
                    ? List.of() : List.of(new PageContent(null, text));
            log.info("Extracted {} characters from RTF", text.length());
            return new ExtractionResult(pages, text);
        } catch (BadLocationException e) {
            throw new IOException("Failed to extract text from RTF document", e);
        }
    }

    /**
     * Extracts text from an XLSX (Office Open XML) spreadsheet.
     *
     * <p>Each sheet is treated as a separate page. Cells are joined by tabs,
     * rows by newlines.</p>
     *
     * @param inputStream the XLSX file input stream
     * @return an extraction result with each sheet as a page
     * @throws IOException if the spreadsheet cannot be read
     */
    public ExtractionResult extractXlsx(InputStream inputStream) throws IOException {
        log.debug("Extracting text from XLSX");
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            return extractSpreadsheet(workbook);
        }
    }

    /**
     * Extracts text from a legacy XLS (BIFF) spreadsheet.
     *
     * <p>Each sheet is treated as a separate page. Cells are joined by tabs,
     * rows by newlines.</p>
     *
     * @param inputStream the XLS file input stream
     * @return an extraction result with each sheet as a page
     * @throws IOException if the spreadsheet cannot be read
     */
    public ExtractionResult extractXls(InputStream inputStream) throws IOException {
        log.debug("Extracting text from XLS");
        try (HSSFWorkbook workbook = new HSSFWorkbook(inputStream)) {
            return extractSpreadsheet(workbook);
        }
    }

    /**
     * Extracts text from a PPTX (Office Open XML) presentation.
     *
     * <p>Each slide is treated as a separate page. Text is extracted from
     * all text shapes on each slide.</p>
     *
     * @param inputStream the PPTX file input stream
     * @return an extraction result with each slide as a page
     * @throws IOException if the presentation cannot be read
     */
    public ExtractionResult extractPptx(InputStream inputStream) throws IOException {
        log.debug("Extracting text from PPTX");
        List<PageContent> pages = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        try (XMLSlideShow pptx = new XMLSlideShow(inputStream)) {
            int slideNum = 1;
            for (XSLFSlide slide : pptx.getSlides()) {
                StringBuilder slideText = new StringBuilder();
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = sanitize(textShape.getText());
                        if (text != null && !text.isBlank()) {
                            slideText.append(text).append("\n");
                        }
                    }
                }
                String text = slideText.toString().trim();
                if (!text.isEmpty()) {
                    pages.add(new PageContent(slideNum, text));
                    fullText.append(text).append("\n");
                }
                slideNum++;
            }
        }
        String result = fullText.toString().trim();
        log.info("Extracted {} slides from PPTX", pages.size());
        return new ExtractionResult(pages, result);
    }

    /**
     * Extracts text from a legacy PPT (BIFF) presentation.
     *
     * <p>Uses {@link SlideShowExtractor} to extract all text from the
     * slide show. The entire text is returned as a single page.</p>
     *
     * @param inputStream the PPT file input stream
     * @return an extraction result with the full text as a single page
     * @throws IOException if the presentation cannot be read
     */
    public ExtractionResult extractPpt(InputStream inputStream) throws IOException {
        log.debug("Extracting text from PPT");
        String text;
        try (HSLFSlideShow ppt = new HSLFSlideShow(inputStream);
             SlideShowExtractor<HSLFShape, HSLFTextParagraph> extractor =
                     new SlideShowExtractor<>(ppt)) {
            text = sanitize(extractor.getText().trim());
        }
        List<PageContent> pages = text.isEmpty()
                ? List.of() : List.of(new PageContent(null, text));
        log.info("Extracted {} characters from PPT", text.length());
        return new ExtractionResult(pages, text);
    }

    /**
     * Strips null bytes ({@code 0x00}) from text to prevent PostgreSQL
     * "invalid byte sequence for encoding UTF8" errors.
     */
    private String sanitize(String text) {
        if (text == null) return null;
        return text.replace("\u0000", "");
    }

    private ExtractionResult extractSpreadsheet(Workbook workbook) {
        List<PageContent> pages = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        DataFormatter formatter = new DataFormatter();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            StringBuilder sheetText = new StringBuilder();
            for (Row row : sheet) {
                StringBuilder rowText = new StringBuilder();
                for (Cell cell : row) {
                    String value = sanitize(formatter.formatCellValue(cell));
                    if (!value.isBlank()) {
                        if (rowText.length() > 0) {
                            rowText.append("\t");
                        }
                        rowText.append(value);
                    }
                }
                if (rowText.length() > 0) {
                    sheetText.append(rowText).append("\n");
                }
            }
            String text = sheetText.toString().trim();
            if (!text.isEmpty()) {
                pages.add(new PageContent(i + 1, text));
                fullText.append(text).append("\n");
            }
        }
        String result = fullText.toString().trim();
        log.info("Extracted {} sheets from spreadsheet", pages.size());
        return new ExtractionResult(pages, result);
    }
}
