package com.myoffgridai.knowledge.service;

import com.myoffgridai.common.exception.OcrException;
import com.myoffgridai.knowledge.dto.ExtractionResult;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OcrServiceTest {

    @Mock private Tesseract tesseract;
    private OcrService ocrService;

    @BeforeEach
    void setUp() {
        ocrService = new OcrService(tesseract);
    }

    @Test
    void extractFromImage_withValidImage_returnsText() throws Exception {
        when(tesseract.doOCR(any(java.awt.image.BufferedImage.class)))
                .thenReturn("Extracted OCR text");

        InputStream is = createTestImageStream();
        ExtractionResult result = ocrService.extractFromImage(is);

        assertThat(result.fullText()).isEqualTo("Extracted OCR text");
        assertThat(result.pages()).hasSize(1);
        assertThat(result.pages().get(0).pageNumber()).isNull();
    }

    @Test
    void extractFromImage_tesseractFails_throwsOcrException() throws Exception {
        when(tesseract.doOCR(any(java.awt.image.BufferedImage.class)))
                .thenThrow(new TesseractException("OCR failed"));

        InputStream is = createTestImageStream();

        assertThatThrownBy(() -> ocrService.extractFromImage(is))
                .isInstanceOf(OcrException.class)
                .hasMessageContaining("OCR processing failed");
    }

    @Test
    void extractFromImage_invalidImage_throwsOcrException() {
        InputStream is = new ByteArrayInputStream("not an image".getBytes());

        assertThatThrownBy(() -> ocrService.extractFromImage(is))
                .isInstanceOf(OcrException.class)
                .hasMessageContaining("unsupported or corrupt format");
    }

    @Test
    void extractFromImage_emptyOcrResult_returnsEmpty() throws Exception {
        when(tesseract.doOCR(any(java.awt.image.BufferedImage.class)))
                .thenReturn("   ");

        InputStream is = createTestImageStream();
        ExtractionResult result = ocrService.extractFromImage(is);

        assertThat(result.fullText()).isEmpty();
    }

    private InputStream createTestImageStream() throws Exception {
        BufferedImage image = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 100, 50);
        g.setColor(Color.BLACK);
        g.drawString("Test", 10, 30);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }
}
