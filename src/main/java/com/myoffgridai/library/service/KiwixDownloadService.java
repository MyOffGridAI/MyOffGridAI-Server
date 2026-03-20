package com.myoffgridai.library.service;

import com.myoffgridai.library.config.LibraryProperties;
import com.myoffgridai.library.dto.KiwixCatalogDownloadRequest;
import com.myoffgridai.library.dto.KiwixDownloadState;
import com.myoffgridai.library.dto.KiwixDownloadStatusDto;
import com.myoffgridai.library.model.ZimFile;
import com.myoffgridai.library.repository.ZimFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Downloads ZIM files from the Kiwix catalog into the local ZIM directory.
 *
 * <p>Follows the {@code ModelDownloadService} pattern: streaming downloads via
 * {@link DataBuffer} and {@link AsynchronousFileChannel} — never buffers the full
 * file in memory (ZIM files can be 90+ GB). Progress is tracked in a
 * {@link ConcurrentHashMap}.</p>
 *
 * <p>On completion, creates a {@link ZimFile} entity and calls
 * {@link KiwixProcessService#restart()} to reload kiwix-serve.</p>
 */
@Service
public class KiwixDownloadService {

    private static final Logger log = LoggerFactory.getLogger(KiwixDownloadService.class);

    private final WebClient webClient;
    private final LibraryProperties libraryProperties;
    private final ZimFileRepository zimFileRepository;
    private final KiwixProcessService kiwixProcessService;

    private final ConcurrentHashMap<String, KiwixDownloadStatusDto> downloads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    /**
     * Constructs the Kiwix download service.
     *
     * @param libraryProperties    library configuration (ZIM directory path)
     * @param zimFileRepository    the ZIM file repository
     * @param kiwixProcessService  the kiwix process manager for restart on completion
     */
    public KiwixDownloadService(LibraryProperties libraryProperties,
                                 ZimFileRepository zimFileRepository,
                                 KiwixProcessService kiwixProcessService) {
        HttpClient httpClient = HttpClient.create()
                .followRedirect(true)
                .responseTimeout(java.time.Duration.ofHours(12));
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1))
                .build();
        this.libraryProperties = libraryProperties;
        this.zimFileRepository = zimFileRepository;
        this.kiwixProcessService = kiwixProcessService;
    }

    /**
     * Starts a ZIM file download. Returns the download ID immediately.
     * The download executes asynchronously.
     *
     * @param request the download request with URL and metadata
     * @param userId  the ID of the user who initiated the download
     * @return the download ID for progress tracking
     */
    public String startDownload(KiwixCatalogDownloadRequest request, UUID userId) {
        String downloadId = UUID.randomUUID().toString();

        KiwixDownloadStatusDto initial = new KiwixDownloadStatusDto(
                downloadId, request.filename(), request.sizeBytes(),
                0, 0.0, KiwixDownloadState.QUEUED, null
        );
        downloads.put(downloadId, initial);
        cancelFlags.put(downloadId, new AtomicBoolean(false));

        executeDownload(downloadId, request, userId);
        return downloadId;
    }

    /**
     * Returns current progress for a download.
     *
     * @param downloadId the download identifier
     * @return the current progress, or empty if not found
     */
    public Optional<KiwixDownloadStatusDto> getProgress(String downloadId) {
        return Optional.ofNullable(downloads.get(downloadId));
    }

    /**
     * Returns all active and recent downloads.
     *
     * @return list of all tracked downloads
     */
    public List<KiwixDownloadStatusDto> getAllDownloads() {
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
            log.info("Cancel requested for Kiwix download: {}", downloadId);
        }
    }

    /**
     * Executes the download in an async thread.
     *
     * @param downloadId the download identifier
     * @param request    the download request
     * @param userId     the user who initiated the download
     */
    @Async
    public void executeDownload(String downloadId, KiwixCatalogDownloadRequest request, UUID userId) {
        try {
            Path zimDir = Path.of(libraryProperties.getZimDirectory());
            Files.createDirectories(zimDir);
            Path target = zimDir.resolve(request.filename());

            downloads.put(downloadId, new KiwixDownloadStatusDto(
                    downloadId, request.filename(), request.sizeBytes(),
                    0, 0.0, KiwixDownloadState.DOWNLOADING, null
            ));

            Flux<DataBuffer> body = webClient.get()
                    .uri(request.downloadUrl())
                    .exchangeToFlux(response -> {
                        long contentLength = response.headers().contentLength().orElse(-1);
                        long totalBytes = contentLength > 0 ? contentLength : request.sizeBytes();
                        downloads.put(downloadId, new KiwixDownloadStatusDto(
                                downloadId, request.filename(), totalBytes,
                                0, 0.0, KiwixDownloadState.DOWNLOADING, null
                        ));
                        return response.bodyToFlux(DataBuffer.class);
                    });

            AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                    target,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            long[] bytesWritten = {0};
            long[] lastEmitTime = {System.currentTimeMillis()};

            body.doOnNext(buffer -> {
                try {
                    int readable = buffer.readableByteCount();
                    byte[] bytes = new byte[readable];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);

                    java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(bytes);
                    channel.write(bb, bytesWritten[0]).get();
                    bytesWritten[0] += readable;

                    // Emit progress every 500ms
                    long now = System.currentTimeMillis();
                    if (now - lastEmitTime[0] > 500) {
                        lastEmitTime[0] = now;
                        KiwixDownloadStatusDto current = downloads.get(downloadId);
                        long total = current != null ? current.totalBytes() : request.sizeBytes();
                        double pct = total > 0 ? (double) bytesWritten[0] / total * 100 : 0;

                        downloads.put(downloadId, new KiwixDownloadStatusDto(
                                downloadId, request.filename(), total,
                                bytesWritten[0], pct, KiwixDownloadState.DOWNLOADING, null
                        ));
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

            // Download complete — create ZIM entity
            KiwixDownloadStatusDto current = downloads.get(downloadId);
            long total = current != null ? current.totalBytes() : bytesWritten[0];
            downloads.put(downloadId, new KiwixDownloadStatusDto(
                    downloadId, request.filename(), total,
                    bytesWritten[0], 100.0, KiwixDownloadState.COMPLETE, null
            ));

            ZimFile zimFile = new ZimFile();
            zimFile.setFilename(request.filename());
            zimFile.setDisplayName(request.displayName());
            zimFile.setCategory(request.category());
            zimFile.setLanguage(request.language());
            zimFile.setFileSizeBytes(bytesWritten[0]);
            zimFile.setFilePath(target.toString());
            zimFile.setUploadedBy(userId);
            zimFileRepository.save(zimFile);

            log.info("Kiwix download completed: {} → {}", request.filename(), target);

            // Restart kiwix-serve to pick up the new ZIM file
            try {
                kiwixProcessService.restart();
            } catch (Exception e) {
                log.warn("Failed to restart kiwix-serve after download: {}", e.getMessage());
            }

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            AtomicBoolean flag = cancelFlags.get(downloadId);
            KiwixDownloadState state = (flag != null && flag.get())
                    ? KiwixDownloadState.FAILED : KiwixDownloadState.FAILED;

            KiwixDownloadStatusDto current = downloads.get(downloadId);
            downloads.put(downloadId, new KiwixDownloadStatusDto(
                    downloadId,
                    current != null ? current.filename() : request.filename(),
                    current != null ? current.totalBytes() : 0,
                    current != null ? current.downloadedBytes() : 0,
                    current != null ? current.percentComplete() : 0,
                    state,
                    errorMsg
            ));

            log.error("Kiwix download failed: {} — {}", downloadId, errorMsg);
        } finally {
            cancelFlags.remove(downloadId);
        }
    }
}
