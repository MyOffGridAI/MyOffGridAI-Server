package com.myoffgridai.library.controller;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.library.dto.*;
import com.myoffgridai.library.model.Ebook;
import com.myoffgridai.library.model.EbookFormat;
import com.myoffgridai.library.service.EbookService;
import com.myoffgridai.library.service.GutenbergService;
import com.myoffgridai.library.service.ZimFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    /**
     * Constructs the library controller.
     *
     * @param zimFileService    the ZIM file management service
     * @param ebookService      the eBook management service
     * @param gutenbergService  the Project Gutenberg integration service
     */
    public LibraryController(ZimFileService zimFileService,
                             EbookService ebookService,
                             GutenbergService gutenbergService) {
        this.zimFileService = zimFileService;
        this.ebookService = ebookService;
        this.gutenbergService = gutenbergService;
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
     * Lists eBooks with optional search and format filter.
     *
     * @param search the search term (optional)
     * @param format the format filter (optional)
     * @param page   the page number (0-based, default 0)
     * @param size   the page size (default 20)
     * @return 200 OK with paginated eBook DTOs
     */
    @GetMapping("/ebooks")
    public ResponseEntity<ApiResponse<List<EbookDto>>> listEbooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EbookFormat format,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Page<EbookDto> result = ebookService.list(search, format, PageRequest.of(page, size));
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
