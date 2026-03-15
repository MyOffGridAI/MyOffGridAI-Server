package com.myoffgridai.knowledge.service;

import com.myoffgridai.knowledge.dto.ExtractionResult;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFTextBox;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();

        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        ExtractionResult result = ingestionService.extractPdf(is);

        assertThat(result.pages()).isNotEmpty();
        assertThat(result.fullText()).contains("Hello PDF World");
        assertThat(result.pages().get(0).pageNumber()).isEqualTo(1);
    }

    @Test
    void extractDocx_extractsParagraphs() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("Hello DOCX World");
            doc.createParagraph().createRun().setText("Second paragraph");
            doc.write(baos);
        }

        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        ExtractionResult result = ingestionService.extractDocx(is);

        assertThat(result.fullText()).contains("Hello DOCX World");
        assertThat(result.fullText()).contains("Second paragraph");
        assertThat(result.pages()).hasSize(1);
    }

    @Test
    void extractXlsx_extractsSheetsAsPages() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet1 = wb.createSheet("Sheet1");
            Row row1 = sheet1.createRow(0);
            row1.createCell(0).setCellValue("Name");
            row1.createCell(1).setCellValue("Value");
            Row row2 = sheet1.createRow(1);
            row2.createCell(0).setCellValue("Item1");
            row2.createCell(1).setCellValue(42);

            Sheet sheet2 = wb.createSheet("Sheet2");
            sheet2.createRow(0).createCell(0).setCellValue("Sheet2 data");

            wb.write(baos);
        }

        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        ExtractionResult result = ingestionService.extractXlsx(is);

        assertThat(result.pages()).hasSize(2);
        assertThat(result.fullText()).contains("Name");
        assertThat(result.fullText()).contains("Item1");
        assertThat(result.fullText()).contains("Sheet2 data");
        assertThat(result.pages().get(0).pageNumber()).isEqualTo(1);
        assertThat(result.pages().get(1).pageNumber()).isEqualTo(2);
    }

    @Test
    void extractXls_extractsSheetsAsPages() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Data");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("Hello XLS");
            row.createCell(1).setCellValue(99.5);
            wb.write(baos);
        }

        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        ExtractionResult result = ingestionService.extractXls(is);

        assertThat(result.pages()).hasSize(1);
        assertThat(result.fullText()).contains("Hello XLS");
    }

    @Test
    void extractPptx_extractsSlidesAsPages() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XMLSlideShow pptx = new XMLSlideShow()) {
            XSLFSlide slide1 = pptx.createSlide();
            XSLFTextBox textBox1 = slide1.createTextBox();
            textBox1.setText("Slide 1 Title");

            XSLFSlide slide2 = pptx.createSlide();
            XSLFTextBox textBox2 = slide2.createTextBox();
            textBox2.setText("Slide 2 Content");

            pptx.write(baos);
        }

        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        ExtractionResult result = ingestionService.extractPptx(is);

        assertThat(result.pages()).hasSize(2);
        assertThat(result.fullText()).contains("Slide 1 Title");
        assertThat(result.fullText()).contains("Slide 2 Content");
        assertThat(result.pages().get(0).pageNumber()).isEqualTo(1);
        assertThat(result.pages().get(1).pageNumber()).isEqualTo(2);
    }

    @Test
    void extractPpt_extractsText() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (HSLFSlideShow ppt = new HSLFSlideShow()) {
            HSLFSlide slide = ppt.createSlide();
            HSLFTextBox textBox = slide.createTextBox();
            textBox.setText("Legacy PPT Content");
            ppt.write(baos);
        }

        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        ExtractionResult result = ingestionService.extractPpt(is);

        assertThat(result.fullText()).contains("Legacy PPT Content");
        assertThat(result.pages()).isNotEmpty();
    }
}
