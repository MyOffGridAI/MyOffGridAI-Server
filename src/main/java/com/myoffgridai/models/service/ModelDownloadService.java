package com.myoffgridai.models.service;

import com.myoffgridai.ai.service.InferenceService;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.models.dto.*;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Downloads GGUF and MLX model files from HuggingFace directly into
 * LM Studio's local model directory.
 *
 * <p>Downloads are tracked in-memory (no database entity needed — downloads
 * are transient operations). Progress is emitted as SSE events via
 * {@link ModelDownloadProgressRegistry}.</p>
 *
 * <p>Downloads are resumable: if a partial file exists at the target path,
 * the service uses HTTP Range requests to resume from the last byte.</p>
 *
 * <p>After successful download, the LM Studio API is notified to rescan
 * its model directory.</p>
 */
@Service
public class ModelDownloadService {

    private static final Logger log = LoggerFactory.getLogger(ModelDownloadService.class);

    private final WebClient webClient;
    private final ExternalApiSettingsService settingsService;
    private final ModelDownloadProgressRegistry progressRegistry;
    private final InferenceService inferenceService;
    private final String modelsDirectory;
    private final String lmStudioApiUrl;

    private final ConcurrentHashMap<String, DownloadProgress> downloads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    /**
     * Constructs the service.
     *
     * @param webClientBuilder  the WebClient builder
     * @param settingsService   the external API settings service for HuggingFace token
     * @param progressRegistry  the SSE progress emitter registry
     * @param inferenceService  the inference service for active model info
     * @param modelsDirectory   the local models directory path
     * @param lmStudioApiUrl    the LM Studio API URL for rescan notification
     */
    public ModelDownloadService(WebClient.Builder webClientBuilder,
                                ExternalApiSettingsService settingsService,
                                ModelDownloadProgressRegistry progressRegistry,
                                InferenceService inferenceService,
                                @Value("${app.huggingface.models-directory}") String modelsDirectory,
                                @Value("${app.huggingface.lmstudio-api-url}") String lmStudioApiUrl) {
        this.webClient = webClientBuilder.build();
        this.settingsService = settingsService;
        this.progressRegistry = progressRegistry;
        this.inferenceService = inferenceService;
        this.modelsDirectory = modelsDirectory;
        this.lmStudioApiUrl = lmStudioApiUrl;
    }

    /**
     * Starts a download. Returns the download ID immediately.
     * The download runs asynchronously in a separate thread.
     *
     * @param repoId   the HuggingFace repository ID
     * @param filename the file to download
     * @return the download ID for progress tracking
     */
    public String startDownload(String repoId, String filename) {
        String downloadId = UUID.randomUUID().toString();
        String[] repoParts = repoId.split("/", 2);
        String author = repoParts.length > 1 ? repoParts[0] : "unknown";
        String modelId = repoParts.length > 1 ? repoParts[1] : repoId;

        String targetPath = Path.of(modelsDirectory, author, modelId, filename).toString();

        DownloadProgress initial = new DownloadProgress(
                downloadId, repoId, filename, DownloadStatus.QUEUED,
                0, 0, 0.0, 0.0, 0, targetPath, null
        );
        downloads.put(downloadId, initial);
        cancelFlags.put(downloadId, new AtomicBoolean(false));

        executeDownload(downloadId, repoId, filename, targetPath);
        return downloadId;
    }

    /**
     * Returns current progress for a download.
     *
     * @param downloadId the download identifier
     * @return the current progress, or empty if not found
     */
    public Optional<DownloadProgress> getProgress(String downloadId) {
        return Optional.ofNullable(downloads.get(downloadId));
    }

    /**
     * Returns all active/recent downloads.
     *
     * @return list of all tracked downloads
     */
    public List<DownloadProgress> getAllDownloads() {
        return new ArrayList<>(downloads.values());
    }

    /**
     * Cancels an in-progress download.
     *
     * @param downloadId the download identifier
     */
    public void cancelDownload(String downloadId) {
        AtomicBoolean flag = cancelFlags.get(downloadId);
        if (flag != null) {
            flag.set(true);
            log.info("Cancel requested for download: {}", downloadId);
        }
    }

    /**
     * Returns a list of model files already in the LM Studio models directory.
     *
     * @return list of local model files
     */
    public List<LocalModelFileDto> listLocalModels() {
        Path modelsDir = Path.of(modelsDirectory);
        if (!Files.exists(modelsDir)) {
            return Collections.emptyList();
        }

        String activeModel = null;
        try {
            var active = inferenceService.getActiveModel();
            if (active != null) {
                activeModel = active.name();
            }
        } catch (Exception e) {
            log.debug("Could not determine active model: {}", e.getMessage());
        }

        List<LocalModelFileDto> result = new ArrayList<>();
        String finalActiveModel = activeModel;

        try (var stream = Files.walk(modelsDir, 3)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".gguf") || name.contains("mlx");
                    })
                    .forEach(p -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            Path relative = modelsDir.relativize(p);
                            String repoId = deriveRepoId(relative);
                            String format = p.getFileName().toString().toLowerCase().endsWith(".gguf")
                                    ? "gguf" : "mlx";
                            boolean loaded = finalActiveModel != null
                                    && p.getFileName().toString().contains(finalActiveModel);

                            result.add(new LocalModelFileDto(
                                    p.getFileName().toString(),
                                    repoId,
                                    format,
                                    attrs.size(),
                                    attrs.lastModifiedTime().toInstant(),
                                    loaded
                            ));
                        } catch (IOException e) {
                            log.warn("Could not read file attributes: {}", p, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to scan models directory: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Deletes a local model file from the models directory.
     *
     * @param filename the filename to delete (searched recursively)
     */
    public void deleteLocalModel(String filename) {
        Path modelsDir = Path.of(modelsDirectory);
        try (var stream = Files.walk(modelsDir, 3)) {
            Optional<Path> match = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(filename))
                    .findFirst();

            if (match.isPresent()) {
                Files.delete(match.get());
                log.info("Deleted local model: {}", match.get());
            } else {
                throw new NoSuchFileException("Model file not found: " + filename);
            }
        } catch (NoSuchFileException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete model: " + e.getMessage(), e);
        }
    }

    /**
     * Executes the download in an async thread.
     *
     * @param downloadId the download identifier
     * @param repoId     the HuggingFace repo ID
     * @param filename   the file to download
     * @param targetPath the target file path
     */
    @Async
    public void executeDownload(String downloadId, String repoId, String filename, String targetPath) {
        try {
            Path target = Path.of(targetPath);
            Files.createDirectories(target.getParent());

            long existingSize = Files.exists(target) ? Files.size(target) : 0;
            String downloadUrl = AppConstants.HF_CDN_BASE + "/" + repoId + "/resolve/main/" + filename;

            updateProgress(downloadId, b -> new DownloadProgress(
                    downloadId, repoId, filename, DownloadStatus.DOWNLOADING,
                    existingSize, 0, 0.0, 0.0, 0, targetPath, null
            ));

            var requestSpec = webClient.get().uri(downloadUrl);
            Optional<String> token = settingsService.getHuggingFaceToken();
            if (token.isPresent()) {
                requestSpec = requestSpec.header("Authorization", "Bearer " + token.get());
            }
            if (existingSize > 0) {
                requestSpec = requestSpec.header("Range", "bytes=" + existingSize + "-");
            }

            // Use exchangeToFlux to read headers before consuming body
            Flux<DataBuffer> body = requestSpec.exchangeToFlux(response -> {
                long contentLength = response.headers().contentLength().orElse(-1);
                long totalBytes = contentLength > 0 ? contentLength + existingSize : 0;
                String contentRange = response.headers().header("Content-Range")
                        .stream().findFirst().orElse(null);
                if (contentRange != null && contentRange.contains("/")) {
                    String total = contentRange.substring(contentRange.lastIndexOf('/') + 1);
                    try {
                        totalBytes = Long.parseLong(total);
                    } catch (NumberFormatException ignored) {}
                }
                long finalTotal = totalBytes;
                updateProgress(downloadId, b2 -> new DownloadProgress(
                        downloadId, repoId, filename, DownloadStatus.DOWNLOADING,
                        existingSize, finalTotal, 0.0, 0.0, 0, targetPath, null
                ));
                return response.bodyToFlux(DataBuffer.class);
            });

            Path tempTarget = target;
            OpenOption[] options = existingSize > 0
                    ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND}
                    : new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING};

            AsynchronousFileChannel channel = AsynchronousFileChannel.open(tempTarget, options);

            long[] bytesWritten = {existingSize};
            long[] lastEmitTime = {System.currentTimeMillis()};
            long startTime = System.currentTimeMillis();

            body.doOnNext(buffer -> {
                try {
                    int readable = buffer.readableByteCount();
                    byte[] bytes = new byte[readable];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);

                    java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(bytes);
                    channel.write(bb, bytesWritten[0]).get();
                    bytesWritten[0] += readable;

                    // Emit progress every 64KB
                    long now = System.currentTimeMillis();
                    if (now - lastEmitTime[0] > 500 || bytesWritten[0] % AppConstants.HF_DOWNLOAD_BUFFER_SIZE == 0) {
                        lastEmitTime[0] = now;
                        DownloadProgress current = downloads.get(downloadId);
                        long total = current != null ? current.totalBytes() : 0;
                        double pct = total > 0 ? (double) bytesWritten[0] / total * 100 : 0;
                        double elapsedSec = (now - startTime) / 1000.0;
                        double speed = elapsedSec > 0 ? (bytesWritten[0] - existingSize) / elapsedSec : 0;
                        long eta = speed > 0 ? (long) ((total - bytesWritten[0]) / speed) : 0;

                        DownloadProgress progress = new DownloadProgress(
                                downloadId, repoId, filename, DownloadStatus.DOWNLOADING,
                                bytesWritten[0], total, pct, speed, eta, targetPath, null
                        );
                        downloads.put(downloadId, progress);
                        progressRegistry.emit(downloadId, progress);
                    }

                    // Check cancellation
                    AtomicBoolean flag = cancelFlags.get(downloadId);
                    if (flag != null && flag.get()) {
                        throw new RuntimeException("Download cancelled");
                    }
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Write failed: " + e.getMessage(), e);
                }
            }).blockLast();

            channel.close();

            // Download complete
            DownloadProgress current = downloads.get(downloadId);
            long total = current != null ? current.totalBytes() : bytesWritten[0];
            DownloadProgress completed = new DownloadProgress(
                    downloadId, repoId, filename, DownloadStatus.COMPLETED,
                    bytesWritten[0], total, 100.0, 0, 0, targetPath, null
            );
            downloads.put(downloadId, completed);
            progressRegistry.emit(downloadId, completed);
            progressRegistry.complete(downloadId);

            log.info("Download completed: {} → {}", filename, targetPath);
            notifyLmStudio();

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            AtomicBoolean flag = cancelFlags.get(downloadId);
            DownloadStatus status = (flag != null && flag.get())
                    ? DownloadStatus.CANCELLED : DownloadStatus.FAILED;

            DownloadProgress current = downloads.get(downloadId);
            DownloadProgress failed = new DownloadProgress(
                    downloadId,
                    current != null ? current.repoId() : "",
                    current != null ? current.filename() : "",
                    status,
                    current != null ? current.bytesDownloaded() : 0,
                    current != null ? current.totalBytes() : 0,
                    current != null ? current.percentComplete() : 0,
                    0, 0,
                    current != null ? current.targetPath() : "",
                    errorMsg
            );
            downloads.put(downloadId, failed);
            progressRegistry.emit(downloadId, failed);
            progressRegistry.complete(downloadId);

            if (status == DownloadStatus.CANCELLED) {
                log.info("Download cancelled: {}", downloadId);
            } else {
                log.error("Download failed: {} — {}", downloadId, errorMsg);
            }
        } finally {
            cancelFlags.remove(downloadId);
        }
    }

    private void notifyLmStudio() {
        try {
            webClient.post()
                    .uri(lmStudioApiUrl + "/api/v0/models/rescan")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("LM Studio model rescan triggered");
        } catch (Exception e) {
            log.info("LM Studio rescan endpoint not available (will detect on next startup): {}",
                    e.getMessage());
        }
    }

    private void updateProgress(String downloadId,
                                java.util.function.Function<DownloadProgress, DownloadProgress> updater) {
        downloads.compute(downloadId, (k, v) -> updater.apply(v));
    }

    private String deriveRepoId(Path relativePath) {
        if (relativePath.getNameCount() >= 2) {
            return relativePath.getName(0) + "/" + relativePath.getName(1);
        }
        return null;
    }
}
