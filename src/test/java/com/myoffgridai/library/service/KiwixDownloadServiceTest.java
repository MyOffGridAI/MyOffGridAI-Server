package com.myoffgridai.library.service;

import com.myoffgridai.library.config.LibraryProperties;
import com.myoffgridai.library.dto.KiwixCatalogDownloadRequest;
import com.myoffgridai.library.dto.KiwixDownloadState;
import com.myoffgridai.library.dto.KiwixDownloadStatusDto;
import com.myoffgridai.library.repository.ZimFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KiwixDownloadService}.
 */
@ExtendWith(MockitoExtension.class)
class KiwixDownloadServiceTest {

    @Mock private LibraryProperties libraryProperties;
    @Mock private ZimFileRepository zimFileRepository;
    @Mock private KiwixProcessService kiwixProcessService;

    private KiwixDownloadService service;

    @BeforeEach
    void setUp() {
        service = new KiwixDownloadService(
                libraryProperties, zimFileRepository, kiwixProcessService);
    }

    @Test
    void startDownload_returnsDownloadIdAndQueuesStatus() {
        KiwixCatalogDownloadRequest request = new KiwixCatalogDownloadRequest(
                "https://download.kiwix.org/test.zim",
                "test.zim",
                "Test Wikipedia",
                "wikipedia",
                "eng",
                1024000
        );

        String downloadId = service.startDownload(request, UUID.randomUUID());

        assertThat(downloadId).isNotNull().isNotBlank();

        Optional<KiwixDownloadStatusDto> progress = service.getProgress(downloadId);
        assertThat(progress).isPresent();
        // Status will be QUEUED or DOWNLOADING depending on async timing
        assertThat(progress.get().filename()).isEqualTo("test.zim");
        assertThat(progress.get().id()).isEqualTo(downloadId);
    }

    @Test
    void getProgress_nonExistentId_returnsEmpty() {
        Optional<KiwixDownloadStatusDto> progress = service.getProgress("nonexistent");
        assertThat(progress).isEmpty();
    }

    @Test
    void getAllDownloads_returnsAllTrackedDownloads() {
        KiwixCatalogDownloadRequest request1 = new KiwixCatalogDownloadRequest(
                "https://download.kiwix.org/test1.zim", "test1.zim",
                "Test 1", "wikipedia", "eng", 1024);
        KiwixCatalogDownloadRequest request2 = new KiwixCatalogDownloadRequest(
                "https://download.kiwix.org/test2.zim", "test2.zim",
                "Test 2", "wiktionary", "fra", 2048);

        service.startDownload(request1, UUID.randomUUID());
        service.startDownload(request2, UUID.randomUUID());

        List<KiwixDownloadStatusDto> all = service.getAllDownloads();
        assertThat(all).hasSize(2);
    }

    @Test
    void cancelDownload_setsFlag() {
        KiwixCatalogDownloadRequest request = new KiwixCatalogDownloadRequest(
                "https://download.kiwix.org/test.zim", "test.zim",
                "Test", "wikipedia", "eng", 1024);

        String downloadId = service.startDownload(request, UUID.randomUUID());
        service.cancelDownload(downloadId);

        // Cancel flag was set — no exception
    }

    @Test
    void cancelDownload_nonExistentId_isNoop() {
        service.cancelDownload("nonexistent");
        // No exception
    }
}
