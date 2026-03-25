package com.myoffgridai.library.controller;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.library.dto.*;
import com.myoffgridai.library.model.Ebook;
import com.myoffgridai.library.model.EbookFormat;
import com.myoffgridai.library.service.EbookService;
import com.myoffgridai.library.service.GutenbergService;
import com.myoffgridai.library.service.KiwixCatalogService;
import com.myoffgridai.library.service.KiwixDownloadService;
import com.myoffgridai.library.service.KiwixProcessService;
import com.myoffgridai.library.service.ZimFileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for the offline library system.
 *
 * <p>Provides endpoints for managing ZIM files (Kiwix), eBooks,
 * and Project Gutenberg imports. The library is device-owned:
 * all authenticated users can read, only OWNER/ADMIN can add/remove.</p>
 */
@RestController
@RequestMapping(AppConstants.LIBRARY_API_PATH)
public class LibraryController {

    private static final Logger log = LoggerFactory.getLogger(LibraryController.class);

    private final ZimFileService zimFileService;
    private final EbookService ebookService;
    private final GutenbergService gutenbergService;
    private final KiwixProcessService kiwixProcessService;
    private final KiwixCatalogService kiwixCatalogService;
    private final KiwixDownloadService kiwixDownloadService;

    /**
     * Constructs the library controller.
     *
     * @param zimFileService       the ZIM file management service
     * @param ebookService         the eBook management service
     * @param gutenbergService     the Project Gutenberg integration service
     * @param kiwixProcessService  the kiwix-serve process manager
     * @param kiwixCatalogService  the Kiwix online catalog service
     * @param kiwixDownloadService the Kiwix download service
     */
    public LibraryController(ZimFileService zimFileService,
                             EbookService ebookService,
                             GutenbergService gutenbergService,
                             KiwixProcessService kiwixProcessService,
                             KiwixCatalogService kiwixCatalogService,
                             KiwixDownloadService kiwixDownloadService) {
        this.zimFileService = zimFileService;
        this.ebookService = ebookService;
        this.gutenbergService = gutenbergService;
        this.kiwixProcessService = kiwixProcessService;
        this.kiwixCatalogService = kiwixCatalogService;
        this.kiwixDownloadService = kiwixDownloadService;
    }

    // ── ZIM File Endpoints ───────────────────────────────────────────────────

    /**
     * Uploads a ZIM file to the library.
     *
     * @param principal   the authenticated user
     * @param file        the ZIM file
     * @param displayName the display name for the ZIM content
     * @param category    the content category
     * @return 201 Created with the ZIM file DTO
     */
    @PostMapping(value = "/zim", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ZimFileDto>> uploadZim(
            @AuthenticationPrincipal User principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("displayName") String displayName,
            @RequestParam(value = "category", required = false) String category) {
        log.info("ZIM upload request from user {}: {}", principal.getId(), file.getOriginalFilename());
        ZimFileDto dto = zimFileService.upload(file, displayName, category, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "ZIM file uploaded"));
    }

    /**
     * Lists all ZIM files in the library.
     *
     * @return 200 OK with list of ZIM file DTOs
     */
    @GetMapping("/zim")
    public ResponseEntity<ApiResponse<List<ZimFileDto>>> listZimFiles() {
        List<ZimFileDto> files = zimFileService.listAll();
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    /**
     * Deletes a ZIM file from the library.
     *
     * @param id the ZIM file ID
     * @return 200 OK
     */
    @DeleteMapping("/zim/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteZim(@PathVariable UUID id) {
        zimFileService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "ZIM file deleted"));
    }

    /**
     * Returns the Kiwix server status.
     *
     * @return 200 OK with Kiwix status DTO
     */
    @GetMapping("/kiwix/status")
    public ResponseEntity<ApiResponse<KiwixStatusDto>> kiwixStatus() {
        KiwixStatusDto status = zimFileService.getKiwixStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Returns the Kiwix server URL for client-side content browsing.
     *
     * @return 200 OK with the Kiwix URL string
     */
    @GetMapping("/kiwix/url")
    public ResponseEntity<ApiResponse<String>> kiwixUrl() {
        return ResponseEntity.ok(ApiResponse.success(zimFileService.getKiwixServeUrl()));
    }

    /**
     * Manually triggers kiwix-tools installation (retry after failure).
     *
     * @return 200 OK with status message
     */
    @PostMapping("/kiwix/install")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> installKiwix() {
        log.info("Kiwix install request");
        kiwixProcessService.installKiwix();
        return ResponseEntity.ok(ApiResponse.success(null, "Kiwix installation requested"));
    }

    /**
     * Starts the kiwix-serve process.
     *
     * @return 200 OK with status message
     */
    @PostMapping("/kiwix/start")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> startKiwix() {
        log.info("Kiwix start request");
        kiwixProcessService.start();
        return ResponseEntity.ok(ApiResponse.success(null, "Kiwix start requested"));
    }

    /**
     * Stops the kiwix-serve process.
     *
     * @return 200 OK with status message
     */
    @PostMapping("/kiwix/stop")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> stopKiwix() {
        log.info("Kiwix stop request");
        kiwixProcessService.stop();
        return ResponseEntity.ok(ApiResponse.success(null, "Kiwix stopped"));
    }

    /**
     * Browses the Kiwix online catalog.
     *
     * @param lang     the language filter (optional)
     * @param category the category filter (optional)
     * @param count    the page size (default 20)
     * @param start    the offset (default 0)
     * @return 200 OK with catalog search results
     */
    @GetMapping("/kiwix/catalog/browse")
    public ResponseEntity<ApiResponse<KiwixCatalogSearchResultDto>> browseKiwixCatalog(
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(defaultValue = "0") int start) {
        KiwixCatalogSearchResultDto result = kiwixCatalogService.browse(lang, category, count, start);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Searches the Kiwix online catalog.
     *
     * @param q     the search query
     * @param lang  the language filter (optional)
     * @param count the number of results (default 20)
     * @return 200 OK with search results
     */
    @GetMapping("/kiwix/catalog/search")
    public ResponseEntity<ApiResponse<KiwixCatalogSearchResultDto>> searchKiwixCatalog(
            @RequestParam String q,
            @RequestParam(required = false) String lang,
            @RequestParam(defaultValue = "20") int count) {
        KiwixCatalogSearchResultDto result = kiwixCatalogService.search(q, lang, count);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Starts a ZIM file download from the Kiwix catalog.
     *
     * @param principal the authenticated user
     * @param request   the download request body
     * @return 200 OK with the download ID
     */
    @PostMapping("/kiwix/catalog/download")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> downloadFromCatalog(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody KiwixCatalogDownloadRequest request) {
        log.info("Kiwix catalog download request from user {}: {}", principal.getId(), request.filename());
        String downloadId = kiwixDownloadService.startDownload(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(downloadId, "Download started"));
    }

    /**
     * Lists all active and recent Kiwix downloads.
     *
     * @return 200 OK with download status list
     */
    @GetMapping("/kiwix/downloads")
    public ResponseEntity<ApiResponse<List<KiwixDownloadStatusDto>>> listKiwixDownloads() {
        List<KiwixDownloadStatusDto> downloads = kiwixDownloadService.getAllDownloads();
        return ResponseEntity.ok(ApiResponse.success(downloads));
    }

    /**
     * Returns progress for a specific Kiwix download.
     *
     * @param id the download identifier
     * @return 200 OK with download status, or 404 if not found
     */
    @GetMapping("/kiwix/downloads/{id}")
    public ResponseEntity<ApiResponse<KiwixDownloadStatusDto>> getKiwixDownloadProgress(
            @PathVariable String id) {
        return kiwixDownloadService.getProgress(id)
                .map(status -> ResponseEntity.ok(ApiResponse.success(status)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancels an in-progress Kiwix download.
     *
     * @param id the download identifier
     * @return 200 OK with confirmation message
     */
    @DeleteMapping("/kiwix/downloads/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelKiwixDownload(@PathVariable String id) {
        log.info("Kiwix download cancel request: {}", id);
        kiwixDownloadService.cancelDownload(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Download cancel requested"));
    }

    // ── eBook Endpoints ──────────────────────────────────────────────────────

    /**
     * Uploads an eBook to the library.
     *
     * @param principal the authenticated user
     * @param file      the eBook file
     * @param title     the book title
     * @param author    the author name (optional)
     * @return 201 Created with the eBook DTO
     */
    @PostMapping(value = "/ebooks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EbookDto>> uploadEbook(
            @AuthenticationPrincipal User principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "author", required = false) String author) {
        log.info("eBook upload request from user {}: {}", principal.getId(), file.getOriginalFilename());
        EbookDto dto = ebookService.upload(file, title, author, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "eBook uploaded"));
    }

    /**
     * Lists eBooks with optional search, format filter, and sorting.
     *
     * @param search    the search term (optional)
     * @param format    the format filter (optional)
     * @param page      the page number (0-based, default 0)
     * @param size      the page size (default 20)
     * @param sort      the sort field: title, author, or createdAt (default title)
     * @param direction the sort direction: asc or desc (default asc)
     * @return 200 OK with paginated eBook DTOs
     */
    @GetMapping("/ebooks")
    public ResponseEntity<ApiResponse<List<EbookDto>>> listEbooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EbookFormat format,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);

        if (!List.of("title", "author", "createdAt").contains(sort)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid sort field: " + sort + ". Allowed: title, author, createdAt"));
        }
        if (!List.of("asc", "desc").contains(direction.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid direction: " + direction + ". Allowed: asc, desc"));
        }

        // Map "createdAt" to the entity field "uploadedAt"
        String sortField = "createdAt".equals(sort) ? "uploadedAt" : sort;
        Sort sortSpec = Sort.by(Sort.Direction.fromString(direction), sortField);
        Page<EbookDto> result = ebookService.list(search, format, PageRequest.of(page, size, sortSpec));
        return ResponseEntity.ok(ApiResponse.paginated(
                result.getContent(), result.getTotalElements(), page, size));
    }

    /**
     * Gets a single eBook by ID.
     *
     * @param id the eBook ID
     * @return 200 OK with eBook DTO
     */
    @GetMapping("/ebooks/{id}")
    public ResponseEntity<ApiResponse<EbookDto>> getEbook(@PathVariable UUID id) {
        EbookDto dto = ebookService.get(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * Deletes an eBook from the library.
     *
     * @param id the eBook ID
     * @return 200 OK
     */
    @DeleteMapping("/ebooks/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEbook(@PathVariable UUID id) {
        ebookService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "eBook deleted"));
    }

    /**
     * Downloads an eBook file, incrementing the download count.
     *
     * @param id the eBook ID
     * @return the file as a streaming response
     */
    @GetMapping("/ebooks/{id}/content")
    public ResponseEntity<InputStreamResource> downloadEbook(@PathVariable UUID id) {
        Ebook ebook = ebookService.getForDownload(id);
        Path filePath = Paths.get(ebook.getFilePath());

        try {
            String contentType = switch (ebook.getFormat()) {
                case EPUB -> "application/epub+zip";
                case PDF -> "application/pdf";
                case MOBI -> "application/x-mobipocket-ebook";
                case AZW -> "application/vnd.amazon.ebook";
                case TXT -> "text/plain";
                case HTML -> "text/html";
            };

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", filePath.getFileName().toString());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(new FileInputStream(filePath.toFile())));
        } catch (FileNotFoundException e) {
            log.error("eBook file not found on disk: {}", ebook.getFilePath());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Returns the cover image for an eBook.
     *
     * @param id the eBook ID
     * @return the cover image as JPEG, or 404 if no cover exists
     */
    @GetMapping(value = "/ebooks/{id}/cover", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<Resource> getEbookCover(@PathVariable UUID id) {
        try {
            Resource cover = ebookService.getCoverFile(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(cover);
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Gutenberg Endpoints ──────────────────────────────────────────────────

    /**
     * Browses the Project Gutenberg catalog without a search query.
     *
     * @param sort  the sort order: popular (default), ascending, or descending
     * @param limit the maximum results (default 10)
     * @return 200 OK with browse results
     */
    @GetMapping("/gutenberg/browse")
    public ResponseEntity<ApiResponse<GutenbergSearchResultDto>> browseGutenberg(
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(defaultValue = "10") int limit) {
        GutenbergSearchResultDto results = gutenbergService.browse(sort, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Searches the Project Gutenberg catalog via Gutendex.
     *
     * @param query the search query
     * @param limit the maximum results (default 20)
     * @return 200 OK with search results
     */
    @GetMapping("/gutenberg/search")
    public ResponseEntity<ApiResponse<GutenbergSearchResultDto>> searchGutenberg(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {
        GutenbergSearchResultDto results = gutenbergService.search(query, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Gets metadata for a single Gutenberg book.
     *
     * @param id the Gutenberg book ID
     * @return 200 OK with book metadata
     */
    @GetMapping("/gutenberg/{id}")
    public ResponseEntity<ApiResponse<GutenbergBookDto>> getGutenbergBook(@PathVariable int id) {
        GutenbergBookDto book = gutenbergService.getBookMetadata(id);
        return ResponseEntity.ok(ApiResponse.success(book));
    }

    /**
     * Imports a book from Project Gutenberg into the local library.
     *
     * @param principal the authenticated user
     * @param id        the Gutenberg book ID
     * @return 201 Created with the imported eBook DTO
     */
    @PostMapping("/gutenberg/{id}/import")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EbookDto>> importGutenberg(
            @AuthenticationPrincipal User principal,
            @PathVariable int id) {
        log.info("Gutenberg import request from user {}: book {}", principal.getId(), id);
        EbookDto dto = gutenbergService.importBook(id, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Book imported from Project Gutenberg"));
    }
}
