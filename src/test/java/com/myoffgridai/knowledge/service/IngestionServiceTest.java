package com.myoffgridai.knowledge.service;

import com.myoffgridai.knowledge.dto.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

class IngestionServiceTest {

    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new IngestionService();
    }

    @Test
    void extractText_returnsFullContent() throws IOException {
        String content = "Line one.\nLine two.\nLine three.";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        ExtractionResult result = ingestionService.extractText(is);

        assertThat(result.fullText()).isEqualTo(content);
        assertThat(result.pages()).hasSize(1);
        assertThat(result.pages().get(0).pageNumber()).isNull();
        assertThat(result.pages().get(0).content()).isEqualTo(content);
    }

    @Test
    void extractText_emptyFile_returnsEmpty() throws IOException {
        InputStream is = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

        ExtractionResult result = ingestionService.extractText(is);

        assertThat(result.fullText()).isEmpty();
    }

    @Test
    void extractText_whitespaceOnly_returnsTrimmed() throws IOException {
        InputStream is = new ByteArrayInputStream("   \n\n  ".getBytes(StandardCharsets.UTF_8));

        ExtractionResult result = ingestionService.extractText(is);

        assertThat(result.fullText()).isEmpty();
    }

    @Test
    void extractPdf_withValidPdf_extractsPages() throws IOException {
        // Create a minimal valid PDF in memory using PDFBox 3.x API
        org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument();
        org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
        doc.addPage(page);

        try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                     new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
            cs.beginText();
            cs.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(
                    org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
            cs.newLineAtOffset(100, 700);
            cs.showText("Hello PDF World");
            cs.endText();
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        doc.save(baos);
        doc.close();

        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        ExtractionResult result = ingestionService.extractPdf(is);

        assertThat(result.pages()).isNotEmpty();
        assertThat(result.fullText()).contains("Hello PDF World");
        assertThat(result.pages().get(0).pageNumber()).isEqualTo(1);
    }
}
