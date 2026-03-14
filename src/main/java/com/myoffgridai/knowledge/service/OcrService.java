package com.myoffgridai.knowledge.service;

import com.myoffgridai.common.exception.OcrException;
import com.myoffgridai.knowledge.dto.ExtractionResult;
import com.myoffgridai.knowledge.dto.PageContent;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Performs OCR text extraction from image files using Tesseract.
 *
 * <p>Supports PNG, JPEG, TIFF, and WebP image formats.</p>
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final Tesseract tesseract;

    /**
     * Constructs the OCR service with a default Tesseract instance.
     */
    public OcrService() {
        this.tesseract = new Tesseract();
        this.tesseract.setLanguage("eng");
    }

    /**
     * Constructs the OCR service with a custom Tesseract instance (for testing).
     *
     * @param tesseract the Tesseract instance to use
     */
    OcrService(Tesseract tesseract) {
        this.tesseract = tesseract;
    }

    /**
     * Extracts text from an image file using Tesseract OCR.
     *
     * @param inputStream the image file input stream
     * @return an extraction result containing the OCR text
     * @throws OcrException if OCR processing fails
     */
    public ExtractionResult extractFromImage(InputStream inputStream) {
        log.debug("Performing OCR on image");
        try {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new OcrException("Failed to read image — unsupported or corrupt format");
            }
            String text = tesseract.doOCR(image).trim();
            log.info("OCR extracted {} characters", text.length());
            List<PageContent> pages = List.of(new PageContent(null, text));
            return new ExtractionResult(pages, text);
        } catch (TesseractException e) {
            throw new OcrException("OCR processing failed", e);
        } catch (IOException e) {
            throw new OcrException("Failed to read image for OCR", e);
        }
    }
}
