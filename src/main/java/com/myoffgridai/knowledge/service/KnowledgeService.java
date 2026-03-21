package com.myoffgridai.knowledge.service;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.common.exception.StorageException;
import com.myoffgridai.common.exception.UnsupportedFileTypeException;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.knowledge.dto.DocumentContentDto;
import com.myoffgridai.knowledge.dto.ExtractionResult;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.model.DocumentStatus;
import com.myoffgridai.knowledge.util.DeltaJsonUtils;
import com.myoffgridai.knowledge.model.KnowledgeChunk;
import com.myoffgridai.knowledge.model.KnowledgeDocument;
import com.myoffgridai.knowledge.repository.KnowledgeChunkRepository;
import com.myoffgridai.knowledge.repository.KnowledgeDocumentRepository;
import com.myoffgridai.memory.model.VectorDocument;
import com.myoffgridai.memory.model.VectorSourceType;
import com.myoffgridai.memory.repository.VectorDocumentRepository;
import com.myoffgridai.memory.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.knowledge.dto.UpdateSharingRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the knowledge document lifecycle: upload, async ingestion,
 * chunking, embedding, retrieval, and deletion.
 *
 * <p>On upload, the document is persisted in {@link DocumentStatus#PENDING}
 * and ingestion proceeds asynchronously. The pipeline extracts text (via
 * {@link IngestionService} or {@link OcrService}), chunks it (via
 * {@link ChunkingService}), embeds each chunk (via {@link EmbeddingService}),
 * and stores vector documents for semantic search.</p>
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final VectorDocumentRepository vectorDocumentRepository;
    private final FileStorageService fileStorageService;
    private final IngestionService ingestionService;
    private final OcrService ocrService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final ApplicationContext applicationContext;
    private final UserRepository userRepository;

    /**
     * Constructs the knowledge service.
     *
     * @param documentRepository       the knowledge document repository
     * @param chunkRepository          the knowledge chunk repository
     * @param vectorDocumentRepository the vector document repository
     * @param fileStorageService       the file storage service
     * @param ingestionService         the text extraction service
     * @param ocrService               the OCR service
     * @param chunkingService          the chunking service
     * @param embeddingService         the embedding service
     * @param applicationContext       the Spring application context for proxy lookup
     * @param userRepository           the user repository for owner display names
     */
    public KnowledgeService(KnowledgeDocumentRepository documentRepository,
                            KnowledgeChunkRepository chunkRepository,
                            VectorDocumentRepository vectorDocumentRepository,
                            FileStorageService fileStorageService,
                            IngestionService ingestionService,
                            OcrService ocrService,
                            ChunkingService chunkingService,
                            EmbeddingService embeddingService,
                            ApplicationContext applicationContext,
                            UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.vectorDocumentRepository = vectorDocumentRepository;
        this.fileStorageService = fileStorageService;
        this.ingestionService = ingestionService;
        this.ocrService = ocrService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.applicationContext = applicationContext;
        this.userRepository = userRepository;
    }

    /**
     * Uploads a knowledge document, stores it on disk, and kicks off async ingestion.
     *
     * @param userId the owning user's ID
     * @param file   the uploaded file
     * @return DTO representing the newly created document in PENDING status
     * @throws UnsupportedFileTypeException if the MIME type is not supported
     */
    @Transactional
    public KnowledgeDocumentDto upload(UUID userId, MultipartFile file) {
        String mimeType = file.getContentType();
        if (mimeType == null || !AppConstants.SUPPORTED_MIME_TYPES.contains(mimeType)) {
            throw new UnsupportedFileTypeException(
                    "Unsupported file type: " + mimeType
                            + ". Supported types: " + AppConstants.SUPPORTED_MIME_TYPES);
        }

        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "unknown";

        String storagePath = fileStorageService.store(userId, file, filename);

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setUserId(userId);
        doc.setFilename(filename);
        doc.setMimeType(mimeType);
        doc.setStoragePath(storagePath);
        doc.setFileSizeBytes(file.getSize());
        doc.setStatus(DocumentStatus.PENDING);

        doc = documentRepository.save(doc);
        log.info("Uploaded document: {} ({}), id={}", filename, mimeType, doc.getId());

        scheduleProcessingAfterCommit(doc.getId());

        return toDto(doc, userId);
    }

    /**
     * Processes a document asynchronously: extract text, chunk, embed.
     *
     * @param documentId the ID of the document to process
     */
    @Async
    public void processDocumentAsync(UUID documentId) {
        log.info("Starting async ingestion for document: {}", documentId);
        KnowledgeDocument doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) {
            log.warn("Document not found for async processing: {}", documentId);
            return;
        }

        doc.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(doc);

        try {
            // 1. Extract text
            ExtractionResult extraction = extractText(doc);
            if (extraction.fullText().isBlank()) {
                throw new StorageException("No text could be extracted from document");
            }

            // 1b. Store Delta JSON content if not already set (editor-created docs already have it)
            if (doc.getContent() == null || doc.getContent().isBlank()) {
                doc.setContent(DeltaJsonUtils.textToDeltaJson(extraction.fullText()));
                documentRepository.save(doc);
            }

            // 2. Chunk
            List<String> chunkTexts = chunkingService.chunkText(extraction.fullText());
            log.debug("Produced {} chunks for document {}", chunkTexts.size(), documentId);

            // 3. Save chunks and embed
            List<float[]> embeddings = embeddingService.embedBatch(chunkTexts);

            for (int i = 0; i < chunkTexts.size(); i++) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setDocument(doc);
                chunk.setUserId(doc.getUserId());
                chunk.setChunkIndex(i);
                chunk.setContent(chunkTexts.get(i));
                // Determine page number from extraction pages
                chunk.setPageNumber(resolvePageNumber(extraction, chunkTexts.get(i)));
                chunk = chunkRepository.save(chunk);

                // 4. Store vector embedding
                VectorDocument vectorDoc = new VectorDocument();
                vectorDoc.setUserId(doc.getUserId());
                vectorDoc.setContent(chunkTexts.get(i));
                vectorDoc.setEmbedding(embeddings.get(i));
                vectorDoc.setSourceType(VectorSourceType.KNOWLEDGE_CHUNK);
                vectorDoc.setSourceId(chunk.getId());
                vectorDoc.setMetadata("{\"documentId\":\"" + doc.getId()
                        + "\",\"chunkIndex\":" + i + "}");
                vectorDocumentRepository.save(vectorDoc);
            }

            doc.setChunkCount(chunkTexts.size());
            doc.setStatus(DocumentStatus.READY);
            doc.setProcessedAt(Instant.now());
            documentRepository.save(doc);
            log.info("Document {} processed successfully: {} chunks", documentId, chunkTexts.size());

        } catch (Exception e) {
            log.error("Failed to process document {}: {}", documentId, e.getMessage(), e);
            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage(e.getMessage());
            documentRepository.save(doc);
        }
    }

    /**
     * Lists documents for a user with pagination.
     *
     * @param userId   the user's ID
     * @param pageable the pagination parameters
     * @return a page of document DTOs
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeDocumentDto> listDocuments(UUID userId, Pageable pageable) {
        return listDocuments(userId, "MINE", pageable);
    }

    /**
     * Lists documents for a user with pagination, filtered by scope.
     *
     * @param userId   the user's ID
     * @param scope    "MINE" for user's own docs, "SHARED" for shared docs from other users
     * @param pageable the pagination parameters
     * @return a page of document DTOs
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeDocumentDto> listDocuments(UUID userId, String scope, Pageable pageable) {
        if ("SHARED".equalsIgnoreCase(scope)) {
            return documentRepository.findByIsSharedTrueAndUserIdNotOrderByUploadedAtDesc(userId, pageable)
                    .map(doc -> toDto(doc, userId));
        }
        return documentRepository.findByUserIdOrderByUploadedAtDesc(userId, pageable)
                .map(doc -> toDto(doc, userId));
    }

    /**
     * Gets a single document by ID. Allows access if the user owns it or if it is shared.
     *
     * @param documentId the document ID
     * @param userId     the requesting user's ID
     * @return the document DTO
     * @throws EntityNotFoundException if the document does not exist or is not accessible
     */
    @Transactional(readOnly = true)
    public KnowledgeDocumentDto getDocument(UUID documentId, UUID userId) {
        KnowledgeDocument doc = findDocumentOrShared(documentId, userId);
        return toDto(doc, userId);
    }

    /**
     * Updates the display name of a document.
     *
     * @param documentId  the document ID
     * @param userId      the requesting user's ID
     * @param displayName the new display name
     * @return the updated document DTO
     * @throws EntityNotFoundException if the document does not exist or belongs to another user
     */
    @Transactional
    public KnowledgeDocumentDto updateDisplayName(UUID documentId, UUID userId, String displayName) {
        KnowledgeDocument doc = findDocumentForUser(documentId, userId);
        doc.setDisplayName(displayName);
        doc = documentRepository.save(doc);
        log.info("Updated display name for document {}: {}", documentId, displayName);
        return toDto(doc, userId);
    }

    /**
     * Deletes a document, its chunks, vector embeddings, and stored file.
     *
     * @param documentId the document ID
     * @param userId     the requesting user's ID
     * @throws EntityNotFoundException if the document does not exist or belongs to another user
     */
    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        KnowledgeDocument doc = findDocumentForUser(documentId, userId);

        // Delete vector documents for all chunks of this document
        List<KnowledgeChunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        for (KnowledgeChunk chunk : chunks) {
            vectorDocumentRepository.deleteBySourceIdAndSourceType(
                    chunk.getId(), VectorSourceType.KNOWLEDGE_CHUNK);
        }

        chunkRepository.deleteByDocumentId(documentId);
        fileStorageService.delete(doc.getStoragePath());
        documentRepository.delete(doc);
        log.info("Deleted document {} and {} chunks", documentId, chunks.size());
    }

    /**
     * Retries processing of a failed document.
     *
     * @param documentId the document ID
     * @param userId     the requesting user's ID
     * @return the document DTO after re-queuing
     * @throws EntityNotFoundException  if the document does not exist or belongs to another user
     * @throws IllegalArgumentException if the document is not in FAILED status
     */
    @Transactional
    public KnowledgeDocumentDto retryProcessing(UUID documentId, UUID userId) {
        KnowledgeDocument doc = findDocumentForUser(documentId, userId);
        if (doc.getStatus() != DocumentStatus.FAILED) {
            throw new IllegalArgumentException(
                    "Only FAILED documents can be retried. Current status: " + doc.getStatus());
        }

        // Clean up any partial chunks from the previous attempt
        List<KnowledgeChunk> existingChunks = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        for (KnowledgeChunk chunk : existingChunks) {
            vectorDocumentRepository.deleteBySourceIdAndSourceType(
                    chunk.getId(), VectorSourceType.KNOWLEDGE_CHUNK);
        }
        chunkRepository.deleteByDocumentId(documentId);

        doc.setStatus(DocumentStatus.PENDING);
        doc.setErrorMessage(null);
        doc.setChunkCount(0);
        doc.setProcessedAt(null);
        doc = documentRepository.save(doc);
        log.info("Retrying document processing: {}", documentId);

        scheduleProcessingAfterCommit(doc.getId());

        return toDto(doc, userId);
    }

    /**
     * Gets the chunks for a document, verifying ownership.
     *
     * @param documentId the document ID
     * @param userId     the requesting user's ID
     * @return list of chunks ordered by index
     * @throws EntityNotFoundException if the document does not exist or belongs to another user
     */
    @Transactional(readOnly = true)
    public List<KnowledgeChunk> getChunks(UUID documentId, UUID userId) {
        findDocumentForUser(documentId, userId);
        return chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
    }

    /**
     * Converts a {@link KnowledgeDocument} entity to its DTO representation.
     *
     * @param doc              the entity
     * @param requestingUserId the ID of the user requesting the DTO
     * @return the DTO with ownership and sharing information
     */
    public KnowledgeDocumentDto toDto(KnowledgeDocument doc, UUID requestingUserId) {
        boolean isOwner = doc.getUserId().equals(requestingUserId);
        boolean editable = isOwner && AppConstants.EDITABLE_MIME_TYPES.contains(doc.getMimeType());
        String ownerDisplayName = null;
        if (!isOwner) {
            ownerDisplayName = userRepository.findById(doc.getUserId())
                    .map(User::getDisplayName)
                    .orElse("Unknown");
        }
        return new KnowledgeDocumentDto(
                doc.getId(),
                doc.getFilename(),
                doc.getDisplayName(),
                doc.getMimeType(),
                doc.getFileSizeBytes(),
                doc.getStatus(),
                doc.getErrorMessage(),
                doc.getChunkCount(),
                doc.getUploadedAt(),
                doc.getProcessedAt(),
                doc.getContent() != null && !doc.getContent().isBlank(),
                editable,
                doc.isShared(),
                isOwner,
                ownerDisplayName
        );
    }

    /**
     * Retrieves the content of a document for viewing or editing.
     * Allows access if the user owns the document or if the document is shared.
     *
     * @param documentId the document ID
     * @param userId     the requesting user's ID
     * @return the document content DTO
     * @throws EntityNotFoundException if the document does not exist or is not accessible
     */
    @Transactional(readOnly = true)
    public DocumentContentDto getDocumentContent(UUID documentId, UUID userId) {
        KnowledgeDocument doc = findDocumentOrShared(documentId, userId);
        boolean isOwner = doc.getUserId().equals(userId);
        boolean editable = isOwner && AppConstants.EDITABLE_MIME_TYPES.contains(doc.getMimeType());
        String title = doc.getDisplayName() != null ? doc.getDisplayName() : doc.getFilename();
        return new DocumentContentDto(
                doc.getId(),
                title,
                doc.getContent(),
                doc.getMimeType(),
                editable
        );
    }

    /**
     * Retrieves a document entity for download (streaming the original file).
     * Allows access if the user owns the document or if the document is shared.
     *
     * @param documentId the document ID
     * @param userId     the requesting user's ID
     * @return the knowledge document entity
     * @throws EntityNotFoundException if the document does not exist or is not accessible
     */
    @Transactional(readOnly = true)
    public KnowledgeDocument getDocumentForDownload(UUID documentId, UUID userId) {
        return findDocumentOrShared(documentId, userId);
    }

    /**
     * Creates a new document from the rich text editor.
     *
     * <p>Stores the plain text as a file, sets the MIME type to
     * {@link AppConstants#MIME_QUILL_DELTA}, and triggers async processing.</p>
     *
     * @param userId          the owning user's ID
     * @param title           the document title
     * @param deltaJsonContent the Quill Delta JSON content
     * @return DTO representing the newly created document
     */
    @Transactional
    public KnowledgeDocumentDto createFromEditor(UUID userId, String title, String deltaJsonContent) {
        String plainText = DeltaJsonUtils.deltaJsonToText(deltaJsonContent);
        byte[] bytes = plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = title.replaceAll("[^a-zA-Z0-9._-]", "_") + ".txt";
        String storagePath = fileStorageService.storeBytes(userId, bytes, filename);

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setUserId(userId);
        doc.setFilename(filename);
        doc.setDisplayName(title);
        doc.setMimeType(AppConstants.MIME_QUILL_DELTA);
        doc.setStoragePath(storagePath);
        doc.setFileSizeBytes(bytes.length);
        doc.setStatus(DocumentStatus.PENDING);
        doc.setContent(deltaJsonContent);

        doc = documentRepository.save(doc);
        log.info("Created editor document: {} ({}), id={}", title, filename, doc.getId());

        scheduleProcessingAfterCommit(doc.getId());

        return toDto(doc, userId);
    }

    /**
     * Updates a document's content from the rich text editor.
     *
     * <p>Clears existing chunks and vector embeddings, updates the stored
     * content, and triggers re-processing.</p>
     *
     * @param documentId       the document ID
     * @param userId           the requesting user's ID
     * @param deltaJsonContent the updated Quill Delta JSON content
     * @return the updated document DTO
     * @throws EntityNotFoundException if the document does not exist or belongs to another user
     */
    @Transactional
    public KnowledgeDocumentDto updateContent(UUID documentId, UUID userId, String deltaJsonContent) {
        KnowledgeDocument doc = findDocumentForUser(documentId, userId);

        // Clear existing chunks and vectors
        List<KnowledgeChunk> existingChunks = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        for (KnowledgeChunk chunk : existingChunks) {
            vectorDocumentRepository.deleteBySourceIdAndSourceType(
                    chunk.getId(), VectorSourceType.KNOWLEDGE_CHUNK);
        }
        chunkRepository.deleteByDocumentId(documentId);

        // Update content
        doc.setContent(deltaJsonContent);
        doc.setStatus(DocumentStatus.PENDING);
        doc.setErrorMessage(null);
        doc.setChunkCount(0);
        doc.setProcessedAt(null);

        // Update the stored file with new plain text
        String plainText = DeltaJsonUtils.deltaJsonToText(deltaJsonContent);
        byte[] bytes = plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        doc.setFileSizeBytes(bytes.length);

        doc = documentRepository.save(doc);
        log.info("Updated content for document {}, re-processing", documentId);

        scheduleProcessingAfterCommit(doc.getId());

        return toDto(doc, userId);
    }

    /**
     * Deletes all knowledge documents, chunks, vector embeddings, and stored files
     * for a user. Used by privacy wipe.
     *
     * @param userId the user ID
     */
    @Transactional
    public void deleteAllForUser(UUID userId) {
        log.info("Deleting all knowledge data for user {}", userId);
        vectorDocumentRepository.deleteByUserId(userId);
        chunkRepository.deleteByUserId(userId);

        List<KnowledgeDocument> documents = documentRepository
                .findByUserIdOrderByUploadedAtDesc(userId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();
        for (KnowledgeDocument doc : documents) {
            try {
                fileStorageService.delete(doc.getStoragePath());
            } catch (Exception e) {
                log.warn("Failed to delete file for document {}: {}", doc.getId(), e.getMessage());
            }
        }
        documentRepository.deleteByUserId(userId);
        log.info("Deleted all knowledge data for user {}", userId);
    }

    /**
     * Schedules async document processing to run after the current transaction commits.
     *
     * <p>Uses {@link TransactionSynchronization#afterCommit()} to ensure the document
     * row is visible to the async thread. Invokes the {@code @Async} method through the
     * Spring proxy to ensure the AOP advice (async execution) is applied.</p>
     *
     * @param documentId the ID of the document to process
     */
    private void scheduleProcessingAfterCommit(UUID documentId) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        applicationContext.getBean(KnowledgeService.class)
                                .processDocumentAsync(documentId);
                    }
                });
    }

    /**
     * Updates the sharing status of a document. Only the owner can change sharing.
     *
     * @param documentId the document ID
     * @param userId     the requesting user's ID (must be the owner)
     * @param shared     the new sharing status
     * @return the updated document DTO
     * @throws EntityNotFoundException if the document does not exist or belongs to another user
     */
    @Transactional
    public KnowledgeDocumentDto updateSharing(UUID documentId, UUID userId, boolean shared) {
        KnowledgeDocument doc = findDocumentForUser(documentId, userId);
        doc.setShared(shared);
        doc = documentRepository.save(doc);
        log.info("Updated sharing for document {}: shared={}", documentId, shared);
        return toDto(doc, userId);
    }

    /**
     * Finds a document by ID, first checking ownership, then checking if shared.
     *
     * @param documentId the document ID
     * @param userId     the requesting user's ID
     * @return the document entity
     * @throws EntityNotFoundException if the document is neither owned nor shared
     */
    private KnowledgeDocument findDocumentOrShared(UUID documentId, UUID userId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .or(() -> documentRepository.findByIdAndIsSharedTrue(documentId))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Document not found: " + documentId));
    }

    private KnowledgeDocument findDocumentForUser(UUID documentId, UUID userId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Document not found: " + documentId));
    }

    private ExtractionResult extractText(KnowledgeDocument doc) throws Exception {
        String mimeType = doc.getMimeType();
        try (InputStream is = fileStorageService.getInputStream(doc.getStoragePath())) {
            if ("application/pdf".equals(mimeType)) {
                return ingestionService.extractPdf(is);
            } else if (mimeType.startsWith("image/")) {
                return ocrService.extractFromImage(is);
            } else if (AppConstants.MIME_DOCX.equals(mimeType)) {
                return ingestionService.extractDocx(is);
            } else if (AppConstants.MIME_DOC.equals(mimeType)) {
                return ingestionService.extractDoc(is);
            } else if (AppConstants.MIME_XLSX.equals(mimeType)) {
                return ingestionService.extractXlsx(is);
            } else if (AppConstants.MIME_XLS.equals(mimeType)) {
                return ingestionService.extractXls(is);
            } else if (AppConstants.MIME_PPTX.equals(mimeType)) {
                return ingestionService.extractPptx(is);
            } else if (AppConstants.MIME_PPT.equals(mimeType)) {
                return ingestionService.extractPpt(is);
            } else if (AppConstants.MIME_RTF.equals(mimeType)) {
                return ingestionService.extractRtf(is);
            } else {
                return ingestionService.extractText(is);
            }
        }
    }

    private Integer resolvePageNumber(ExtractionResult extraction, String chunkText) {
        // Find which page contains the start of this chunk
        for (var page : extraction.pages()) {
            if (page.content().contains(chunkText.substring(0, Math.min(50, chunkText.length())))) {
                return page.pageNumber();
            }
        }
        // Default to first page if we can't determine
        return extraction.pages().isEmpty() ? null : extraction.pages().get(0).pageNumber();
    }
}
