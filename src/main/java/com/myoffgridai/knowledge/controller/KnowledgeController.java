package com.myoffgridai.knowledge.controller;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.dto.KnowledgeSearchRequest;
import com.myoffgridai.knowledge.dto.KnowledgeSearchResultDto;
import com.myoffgridai.knowledge.dto.UpdateDisplayNameRequest;
import com.myoffgridai.knowledge.service.KnowledgeService;
import com.myoffgridai.knowledge.service.SemanticSearchService;
import com.myoffgridai.system.service.SystemConfigService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Constructs the knowledge controller.
     *
     * @param knowledgeService       the knowledge service
     * @param semanticSearchService  the semantic search service
     * @param systemConfigService    the system config service
     */
    public KnowledgeController(KnowledgeService knowledgeService,
                                SemanticSearchService semanticSearchService,
                                SystemConfigService systemConfigService) {
        this.knowledgeService = knowledgeService;
        this.semanticSearchService = semanticSearchService;
        this.systemConfigService = systemConfigService;
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
     * Lists the authenticated user's knowledge documents with pagination.
     *
     * @param principal the authenticated user
     * @param page      the page number (0-based, defaults to 0)
     * @param size      the page size (defaults to 20)
     * @return paginated list of document DTOs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<KnowledgeDocumentDto>>> listDocuments(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Page<KnowledgeDocumentDto> result = knowledgeService.listDocuments(
                principal.getId(), PageRequest.of(page, size));
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
