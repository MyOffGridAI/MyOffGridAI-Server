package com.myoffgridai.knowledge.controller;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.knowledge.dto.*;
import com.myoffgridai.knowledge.model.KnowledgeDocument;
import com.myoffgridai.knowledge.service.FileStorageService;
import com.myoffgridai.knowledge.service.KnowledgeService;
import com.myoffgridai.knowledge.service.SemanticSearchService;
import com.myoffgridai.system.service.SystemConfigService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * REST controller for Knowledge Vault operations: upload, list, get, update,
 * delete, retry, and semantic search of knowledge documents.
 */
@RestController
@RequestMapping(AppConstants.KNOWLEDGE_API_PATH)
public class KnowledgeController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);

    private final KnowledgeService knowledgeService;
    private final SemanticSearchService semanticSearchService;
    private final SystemConfigService systemConfigService;
    private final FileStorageService fileStorageService;

    /**
     * Constructs the knowledge controller.
     *
     * @param knowledgeService       the knowledge service
     * @param semanticSearchService  the semantic search service
     * @param systemConfigService    the system config service
     * @param fileStorageService     the file storage service
     */
    public KnowledgeController(KnowledgeService knowledgeService,
                                SemanticSearchService semanticSearchService,
                                SystemConfigService systemConfigService,
                                FileStorageService fileStorageService) {
        this.knowledgeService = knowledgeService;
        this.semanticSearchService = semanticSearchService;
        this.systemConfigService = systemConfigService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Uploads a document for ingestion into the knowledge base.
     *
     * @param principal the authenticated user
     * @param file      the uploaded file
     * @return 201 Created with the document DTO in PENDING status
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<KnowledgeDocumentDto>> uploadDocument(
            @AuthenticationPrincipal User principal,
            @RequestParam("file") MultipartFile file) {
        log.info("Upload request from user {}: {}", principal.getId(), file.getOriginalFilename());

        int maxMb = systemConfigService.getConfig().getMaxUploadSizeMb();
        long maxBytes = (long) maxMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            long fileSizeMb = file.getSize() / (1024 * 1024);
            throw new IllegalArgumentException(
                    String.format("File size (%d MB) exceeds the maximum allowed (%d MB)", fileSizeMb, maxMb));
        }

        KnowledgeDocumentDto dto = knowledgeService.upload(principal.getId(), file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Document uploaded, processing started"));
    }

    /**
     * Lists knowledge documents with pagination, filtered by scope.
     *
     * @param principal the authenticated user
     * @param page      the page number (0-based, defaults to 0)
     * @param size      the page size (defaults to 20)
     * @param scope     "MINE" for user's own docs (default), "SHARED" for shared docs from others
     * @return paginated list of document DTOs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<KnowledgeDocumentDto>>> listDocuments(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "MINE") String scope) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Page<KnowledgeDocumentDto> result = knowledgeService.listDocuments(
                principal.getId(), scope, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.paginated(
                result.getContent(), result.getTotalElements(), page, size));
    }

    /**
     * Gets a single knowledge document by ID.
     *
     * @param principal  the authenticated user
     * @param documentId the document ID
     * @return the document DTO
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<KnowledgeDocumentDto>> getDocument(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID documentId) {
        KnowledgeDocumentDto dto = knowledgeService.getDocument(documentId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * Updates the display name of a knowledge document.
     *
     * @param principal  the authenticated user
     * @param documentId the document ID
     * @param request    the request containing the new display name
     * @return the updated document DTO
     */
    @PutMapping("/{documentId}/display-name")
    public ResponseEntity<ApiResponse<KnowledgeDocumentDto>> updateDisplayName(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID documentId,
            @Valid @RequestBody UpdateDisplayNameRequest request) {
        KnowledgeDocumentDto dto = knowledgeService.updateDisplayName(
                documentId, principal.getId(), request.displayName());
        return ResponseEntity.ok(ApiResponse.success(dto, "Display name updated"));
    }

    /**
     * Updates the sharing status of a knowledge document. Only the owner can share/unshare.
     *
     * @param principal  the authenticated user
     * @param documentId the document ID
     * @param request    the request containing the new sharing status
     * @return the updated document DTO
     */
    @PatchMapping("/{documentId}/sharing")
    public ResponseEntity<ApiResponse<KnowledgeDocumentDto>> updateSharing(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID documentId,
            @Valid @RequestBody UpdateSharingRequest request) {
        log.info("Update sharing request from user {} for document {}: shared={}",
                principal.getId(), documentId, request.shared());
        KnowledgeDocumentDto dto = knowledgeService.updateSharing(
                documentId, principal.getId(), request.shared());
        return ResponseEntity.ok(ApiResponse.success(dto, "Sharing updated"));
    }

    /**
     * Deletes a knowledge document and all associated chunks/embeddings.
     *
     * @param principal  the authenticated user
     * @param documentId the document ID
     * @return success response
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID documentId) {
        knowledgeService.deleteDocument(documentId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted"));
    }

    /**
     * Retries processing of a failed document.
     *
     * @param principal  the authenticated user
     * @param documentId the document ID
     * @return the document DTO after re-queuing
     */
    @PostMapping("/{documentId}/retry")
    public ResponseEntity<ApiResponse<KnowledgeDocumentDto>> retryProcessing(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID documentId) {
        KnowledgeDocumentDto dto = knowledgeService.retryProcessing(
                documentId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto, "Document re-queued for processing"));
    }

    /**
     * Downloads the original file of a knowledge document.
     *
     * @param principal  the authenticated user
     * @param documentId the document ID
     * @return the file as a streaming response with appropriate headers
     */
    @GetMapping("/{documentId}/download")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID documentId) {
        KnowledgeDocument doc = knowledgeService.getDocumentForDownload(documentId, principal.getId());
        InputStream inputStream = fileStorageService.getInputStream(doc.getStoragePath());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(doc.getMimeType()));
        headers.setContentDispositionFormData("attachment", doc.getFilename());

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(inputStream));
    }

    /**
     * Retrieves the content of a knowledge document for viewing or editing.
     *
     * @param principal  the authenticated user
     * @param documentId the document ID
     * @return the document content DTO
     */
    @GetMapping("/{documentId}/content")
    public ResponseEntity<ApiResponse<DocumentContentDto>> getDocumentContent(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID documentId) {
        DocumentContentDto dto = knowledgeService.getDocumentContent(documentId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * Creates a new document from the rich text editor.
     *
     * @param principal the authenticated user
     * @param request   the request containing title and Delta JSON content
     * @return 201 Created with the document DTO
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<KnowledgeDocumentDto>> createDocument(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody CreateDocumentRequest request) {
        log.info("Create document request from user {}: {}", principal.getId(), request.title());
        KnowledgeDocumentDto dto = knowledgeService.createFromEditor(
                principal.getId(), request.title(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Document created, processing started"));
    }

    /**
     * Updates the content of a knowledge document from the rich text editor.
     *
     * @param principal  the authenticated user
     * @param documentId the document ID
     * @param request    the request containing updated Delta JSON content
     * @return the updated document DTO
     */
    @PutMapping("/{documentId}/content")
    public ResponseEntity<ApiResponse<KnowledgeDocumentDto>> updateDocumentContent(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID documentId,
            @Valid @RequestBody UpdateContentRequest request) {
        log.info("Update content request from user {} for document {}", principal.getId(), documentId);
        KnowledgeDocumentDto dto = knowledgeService.updateContent(
                documentId, principal.getId(), request.content());
        return ResponseEntity.ok(ApiResponse.success(dto, "Content updated, re-processing started"));
    }

    /**
     * Performs a semantic search across the user's knowledge base.
     *
     * @param principal the authenticated user
     * @param request   the search request containing query and optional topK
     * @return list of search result DTOs with similarity scores
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<KnowledgeSearchResultDto>>> searchKnowledge(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody KnowledgeSearchRequest request) {
        int topK = request.topK() != null ? request.topK() : AppConstants.RAG_TOP_K;
        List<KnowledgeSearchResultDto> results = semanticSearchService.search(
                principal.getId(), request.query(), topK);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
