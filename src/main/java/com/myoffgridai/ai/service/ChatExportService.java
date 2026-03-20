package com.myoffgridai.ai.service;

import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.model.Message;
import com.myoffgridai.ai.model.MessageRole;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.model.DocumentStatus;
import com.myoffgridai.knowledge.model.KnowledgeDocument;
import com.myoffgridai.knowledge.repository.KnowledgeDocumentRepository;
import com.myoffgridai.knowledge.service.FileStorageService;
import com.myoffgridai.knowledge.service.KnowledgeService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for exporting chat conversations as PDF documents.
 *
 * <p>Generates PDF representations of conversations using PDFBox and
 * optionally saves them to the Knowledge Library for RAG search.</p>
 */
@Service
public class ChatExportService {

    private static final Logger log = LoggerFactory.getLogger(ChatExportService.class);

    private static final float PAGE_MARGIN = 50f;
    private static final float FONT_SIZE_TITLE = 16f;
    private static final float FONT_SIZE_SUBTITLE = 10f;
    private static final float FONT_SIZE_ROLE = 11f;
    private static final float FONT_SIZE_BODY = 10f;
    private static final float LINE_SPACING = 14f;
    private static final float MESSAGE_GAP = 10f;
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final ChatService chatService;
    private final MessageRepository messageRepository;
    private final FileStorageService fileStorageService;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeService knowledgeService;

    /**
     * Constructs the chat export service.
     *
     * @param chatService                 the chat service for ownership verification
     * @param messageRepository           the message repository
     * @param fileStorageService          the file storage service
     * @param knowledgeDocumentRepository the knowledge document repository
     * @param knowledgeService            the knowledge service for async processing
     */
    public ChatExportService(ChatService chatService,
                             MessageRepository messageRepository,
                             FileStorageService fileStorageService,
                             KnowledgeDocumentRepository knowledgeDocumentRepository,
                             KnowledgeService knowledgeService) {
        this.chatService = chatService;
        this.messageRepository = messageRepository;
        this.fileStorageService = fileStorageService;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeService = knowledgeService;
    }

    /**
     * Generates a PDF document from a conversation's messages.
     *
     * <p>The PDF includes a title page header with the conversation title and
     * export date, followed by chronological messages with role labels and
     * timestamps. Text is automatically wrapped to fit the page width.</p>
     *
     * @param conversationId the conversation to export
     * @param userId         the requesting user's ID (for ownership verification)
     * @return the PDF document as a byte array
     * @throws com.myoffgridai.common.exception.EntityNotFoundException if conversation not found or not owned
     */
    public byte[] generateConversationPdf(UUID conversationId, UUID userId) {
        Conversation conversation = chatService.getConversation(conversationId, userId);
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        String title = conversation.getTitle() != null ? conversation.getTitle() : "Chat Export";

        try (PDDocument document = new PDDocument()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float pageWidth = PDRectangle.LETTER.getWidth();
            float usableWidth = pageWidth - 2 * PAGE_MARGIN;

            PDPage currentPage = new PDPage(PDRectangle.LETTER);
            document.addPage(currentPage);
            PDPageContentStream cs = new PDPageContentStream(document, currentPage);
            float yPos = PDRectangle.LETTER.getHeight() - PAGE_MARGIN;

            // Title
            yPos = drawText(cs, fontBold, FONT_SIZE_TITLE, title, PAGE_MARGIN, yPos, usableWidth);
            yPos -= LINE_SPACING;

            // Subtitle
            String subtitle = "MyOffGridAI Chat Export — " + TIMESTAMP_FMT.format(java.time.Instant.now());
            yPos = drawText(cs, fontRegular, FONT_SIZE_SUBTITLE, subtitle, PAGE_MARGIN, yPos, usableWidth);
            yPos -= LINE_SPACING * 2;

            // Messages
            for (Message msg : messages) {
                String roleLabel = msg.getRole() == MessageRole.USER ? "You:" : "Assistant:";
                String timestamp = msg.getCreatedAt() != null
                        ? " (" + TIMESTAMP_FMT.format(msg.getCreatedAt()) + ")"
                        : "";
                String header = roleLabel + timestamp;
                String content = msg.getContent() != null ? msg.getContent() : "";

                // Check if we need a new page for the header
                if (yPos < PAGE_MARGIN + LINE_SPACING * 3) {
                    cs.close();
                    currentPage = new PDPage(PDRectangle.LETTER);
                    document.addPage(currentPage);
                    cs = new PDPageContentStream(document, currentPage);
                    yPos = PDRectangle.LETTER.getHeight() - PAGE_MARGIN;
                }

                // Role header
                yPos = drawText(cs, fontBold, FONT_SIZE_ROLE, header, PAGE_MARGIN, yPos, usableWidth);
                yPos -= 2f;

                // Content lines
                List<String> lines = wrapText(content, fontRegular, FONT_SIZE_BODY, usableWidth);
                for (String line : lines) {
                    if (yPos < PAGE_MARGIN + LINE_SPACING) {
                        cs.close();
                        currentPage = new PDPage(PDRectangle.LETTER);
                        document.addPage(currentPage);
                        cs = new PDPageContentStream(document, currentPage);
                        yPos = PDRectangle.LETTER.getHeight() - PAGE_MARGIN;
                    }
                    yPos = drawText(cs, fontRegular, FONT_SIZE_BODY, line, PAGE_MARGIN, yPos, usableWidth);
                }

                yPos -= MESSAGE_GAP;
            }

            cs.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            log.info("Generated PDF for conversation {} ({} messages, {} bytes)",
                    conversationId, messages.size(), baos.size());
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF for conversation " + conversationId, e);
        }
    }

    /**
     * Generates a PDF from the conversation and saves it to the Knowledge Library.
     *
     * <p>Creates a PDF, stores it via {@link FileStorageService}, persists a
     * {@link KnowledgeDocument} entity, and triggers asynchronous processing
     * for RAG indexing.</p>
     *
     * @param conversationId the conversation to export
     * @param userId         the requesting user's ID
     * @return the created knowledge document DTO
     * @throws com.myoffgridai.common.exception.EntityNotFoundException if conversation not found or not owned
     */
    @Transactional
    public KnowledgeDocumentDto saveConversationToLibrary(UUID conversationId, UUID userId) {
        Conversation conversation = chatService.getConversation(conversationId, userId);
        String title = conversation.getTitle() != null ? conversation.getTitle() : "Chat Export";

        byte[] pdfBytes = generateConversationPdf(conversationId, userId);

        String filename = title.replaceAll("[^a-zA-Z0-9._-]", "_") + ".pdf";
        String storagePath = fileStorageService.storeBytes(userId, pdfBytes, filename);

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setUserId(userId);
        doc.setFilename(filename);
        doc.setDisplayName(title);
        doc.setMimeType("application/pdf");
        doc.setStoragePath(storagePath);
        doc.setFileSizeBytes(pdfBytes.length);
        doc.setStatus(DocumentStatus.PENDING);

        doc = knowledgeDocumentRepository.save(doc);
        log.info("Saved conversation {} to library as document {} ({})",
                conversationId, doc.getId(), filename);

        knowledgeService.processDocumentAsync(doc.getId());

        return knowledgeService.toDto(doc);
    }

    /**
     * Draws a text string at the given position, returning the new Y position.
     */
    private float drawText(PDPageContentStream cs, PDType1Font font, float fontSize,
                           String text, float x, float y, float maxWidth) throws IOException {
        // Sanitize: replace characters not in WinAnsiEncoding
        String safe = sanitizeForPdf(text);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(safe);
        cs.endText();
        return y - LINE_SPACING;
    }

    /**
     * Wraps text to fit within the given width using the specified font and size.
     */
    private List<String> wrapText(String text, PDType1Font font, float fontSize,
                                  float maxWidth) throws IOException {
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\n");

        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                result.add("");
                continue;
            }
            String[] words = paragraph.split("\\s+");
            StringBuilder line = new StringBuilder();

            for (String word : words) {
                String sanitized = sanitizeForPdf(word);
                String candidate = line.isEmpty() ? sanitized : line + " " + sanitized;
                float width = font.getStringWidth(candidate) / 1000f * fontSize;

                if (width > maxWidth && !line.isEmpty()) {
                    result.add(line.toString());
                    line = new StringBuilder(sanitized);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            if (!line.isEmpty()) {
                result.add(line.toString());
            }
        }

        return result;
    }

    /**
     * Replaces characters outside WinAnsiEncoding with a replacement character.
     */
    private String sanitizeForPdf(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c >= 32 && c <= 255) {
                sb.append(c);
            } else if (c == '\t') {
                sb.append("    ");
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }
}
