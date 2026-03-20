package com.myoffgridai.models.controller;

import com.myoffgridai.ai.dto.LlamaServerStatusDto;
import com.myoffgridai.ai.dto.SetActiveModelRequest;
import com.myoffgridai.ai.service.LlamaServerProcessService;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.models.dto.*;
import com.myoffgridai.models.service.ModelCatalogService;
import com.myoffgridai.models.service.ModelDownloadProgressRegistry;
import com.myoffgridai.models.service.ModelDownloadService;
import com.myoffgridai.system.service.SystemConfigService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * REST controller for the HuggingFace model catalog, download management,
 * and local model management.
 *
 * <p>Catalog endpoints are accessible to all authenticated users. Download
 * and delete operations require the OWNER role.</p>
 */
@RestController
@RequestMapping("/api/models")
public class ModelDownloadController {

    private static final Logger log = LoggerFactory.getLogger(ModelDownloadController.class);

    private final ModelCatalogService catalogService;
    private final ModelDownloadService downloadService;
    private final ModelDownloadProgressRegistry progressRegistry;
    private final SystemConfigService systemConfigService;
    private final LlamaServerProcessService llamaServerProcessService;

    /**
     * Constructs the controller.
     *
     * @param catalogService             the HuggingFace catalog service
     * @param downloadService            the model download service
     * @param progressRegistry           the SSE progress registry
     * @param systemConfigService        the system config service
     * @param llamaServerProcessService  the llama-server process service (nullable)
     */
    public ModelDownloadController(ModelCatalogService catalogService,
                                   ModelDownloadService downloadService,
                                   ModelDownloadProgressRegistry progressRegistry,
                                   SystemConfigService systemConfigService,
                                   @Autowired(required = false) LlamaServerProcessService llamaServerProcessService) {
        this.catalogService = catalogService;
        this.downloadService = downloadService;
        this.progressRegistry = progressRegistry;
        this.systemConfigService = systemConfigService;
        this.llamaServerProcessService = llamaServerProcessService;
    }

    // ── Catalog endpoints (authenticated) ────────────────────────────────

    /**
     * Searches HuggingFace for models matching the query.
     *
     * <p>When {@code q} is omitted or empty, returns top models sorted by
     * downloads descending (browse mode).</p>
     *
     * @param q      the search query (optional; defaults to empty = browse mode)
     * @param format the format filter ("gguf", "mlx", or "all")
     * @param limit  the max number of results (default 20)
     * @return the search results
     */
    @GetMapping("/catalog/search")
    public ApiResponse<HfSearchResultDto> searchCatalog(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "gguf") String format,
            @RequestParam(defaultValue = "" + AppConstants.HF_SEARCH_DEFAULT_LIMIT) int limit) {
        log.info("Catalog search: q='{}', format='{}', limit={}", q, format, limit);
        HfSearchResultDto result = catalogService.searchModels(q, format, limit);
        return ApiResponse.success(result);
    }

    /**
     * Returns full model metadata for a HuggingFace model repository.
     *
     * @param author  the repository author/organization
     * @param modelId the model name
     * @return the model details
     */
    @GetMapping("/catalog/{author}/{modelId}")
    public ApiResponse<HfModelDto> getModelDetails(
            @PathVariable String author,
            @PathVariable String modelId) {
        String repoId = author + "/" + modelId;
        log.info("Fetching catalog model details: {}", repoId);
        HfModelDto model = catalogService.getModelDetails(repoId);
        return ApiResponse.success(model);
    }

    /**
     * Returns the file list for a HuggingFace model repository.
     *
     * @param author  the repository author/organization
     * @param modelId the model name
     * @return the list of files
     */
    @GetMapping("/catalog/{author}/{modelId}/files")
    public ApiResponse<List<HfModelFileDto>> getModelFiles(
            @PathVariable String author,
            @PathVariable String modelId) {
        String repoId = author + "/" + modelId;
        log.info("Fetching catalog model files: {}", repoId);
        List<HfModelFileDto> files = catalogService.getModelFiles(repoId);
        return ApiResponse.success(files);
    }

    // ── Download management (OWNER only) ──────────────────────────────

    /**
     * Starts a model file download from HuggingFace.
     *
     * @param request the download request
     * @return the download started response with ID and target path
     */
    @PostMapping("/download")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<DownloadStartedDto> startDownload(@Valid @RequestBody StartDownloadRequest request) {
        log.info("Starting download: {} / {}", request.repoId(), request.filename());
        String downloadId = downloadService.startDownload(request.repoId(), request.filename());
        DownloadProgress progress = downloadService.getProgress(downloadId).orElse(null);
        return ApiResponse.success(new DownloadStartedDto(
                downloadId,
                progress != null ? progress.targetPath() : null,
                progress != null ? progress.totalBytes() : null
        ));
    }

    /**
     * Returns all active and recent downloads.
     *
     * @return the list of downloads
     */
    @GetMapping("/download")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<List<DownloadProgress>> getAllDownloads() {
        return ApiResponse.success(downloadService.getAllDownloads());
    }

    /**
     * Streams download progress via SSE.
     *
     * @param downloadId the download identifier
     * @return an SSE emitter streaming progress events
     */
    @GetMapping(value = "/download/{downloadId}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public SseEmitter getDownloadProgress(@PathVariable String downloadId) {
        log.info("SSE progress subscription for download: {}", downloadId);
        return progressRegistry.subscribe(downloadId);
    }

    /**
     * Cancels an in-progress download.
     *
     * @param downloadId the download identifier
     * @return success response
     */
    @DeleteMapping("/download/{downloadId}")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<Void> cancelDownload(@PathVariable String downloadId) {
        log.info("Cancelling download: {}", downloadId);
        downloadService.cancelDownload(downloadId);
        return ApiResponse.success(null);
    }

    // ── Local model management ────────────────────────────────────────

    /**
     * Returns the list of model files in the local models directory.
     *
     * @return the list of local models
     */
    @GetMapping("/local")
    public ApiResponse<List<LocalModelFileDto>> listLocalModels() {
        return ApiResponse.success(downloadService.listLocalModels());
    }

    /**
     * Deletes a local model file from disk.
     *
     * @param filename the filename to delete
     * @return success response
     */
    @DeleteMapping("/local/{filename}")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<Void> deleteLocalModel(@PathVariable String filename) {
        log.info("Deleting local model: {}", filename);
        downloadService.deleteLocalModel(filename);
        return ApiResponse.success(null);
    }

    // ── Model management endpoints ─────────────────────────────────────

    /**
     * Sets the active model and loads it into the llama-server inference engine.
     *
     * @param request the set active model request
     * @return the inference status after model load
     */
    @PostMapping("/active")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<?> setActiveModel(
            @Valid @RequestBody SetActiveModelRequest request) {
        log.info("Setting active model: {}", request.filename());

        if (llamaServerProcessService != null) {
            LlamaServerStatusDto status = llamaServerProcessService.switchModel(request.filename());
            return ApiResponse.success(status);
        }

        return ApiResponse.error("No inference service available");
    }

    /**
     * Returns the current llama-server inference engine status.
     *
     * @return the engine status
     */
    @GetMapping("/server-status")
    public ApiResponse<?> getServerStatus() {
        if (llamaServerProcessService != null) {
            return ApiResponse.success(llamaServerProcessService.getStatus());
        }
        return ApiResponse.error("No inference service available");
    }

    /**
     * Reloads the currently active model in the llama-server inference engine.
     *
     * @return the engine status after reload
     */
    @PostMapping("/restart")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<?> reloadModel() {
        log.info("Reload model requested");

        if (llamaServerProcessService != null) {
            llamaServerProcessService.restart();
            return ApiResponse.success(llamaServerProcessService.getStatus());
        }

        return ApiResponse.error("No inference service available");
    }
}
