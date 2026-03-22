package com.myoffgridai.system.service;

import com.myoffgridai.system.model.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ApModeStartupService}.
 *
 * <p>Verifies that startup AP mode logic is idempotent and does not create
 * duplicate system configuration rows when invoked multiple times.</p>
 */
@ExtendWith(MockitoExtension.class)
class ApModeStartupServiceTest {

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private ApModeService apModeService;

    private ApModeStartupService startupService;

    @BeforeEach
    void setUp() {
        startupService = new ApModeStartupService(systemConfigService, apModeService);
    }

    @Test
    void onApplicationReady_uninitializedSystem_startsApMode() {
        when(systemConfigService.isInitialized()).thenReturn(false);
        SystemConfig config = new SystemConfig();
        when(systemConfigService.getConfig()).thenReturn(config);
        when(systemConfigService.save(any())).thenReturn(config);

        startupService.onApplicationReady();

        verify(apModeService).startApMode();
        verify(systemConfigService).save(any());
    }

    @Test
    void onApplicationReady_initializedSystem_stopsApMode() {
        when(systemConfigService.isInitialized()).thenReturn(true);

        startupService.onApplicationReady();

        verify(apModeService).stopApMode();
        verify(apModeService, never()).startApMode();
    }

    @Test
    void onApplicationReady_uninitializedSystem_setsApModeEnabled() {
        when(systemConfigService.isInitialized()).thenReturn(false);
        SystemConfig config = new SystemConfig();
        when(systemConfigService.getConfig()).thenReturn(config);
        when(systemConfigService.save(any())).thenReturn(config);

        startupService.onApplicationReady();

        verify(systemConfigService).save(argThat(c -> c.isApModeEnabled()));
    }

    /**
     * Verifies that calling onApplicationReady() multiple times does not
     * create duplicate rows — it always delegates to getConfig() which
     * handles singleton enforcement.
     */
    @Test
    void onApplicationReady_calledMultipleTimes_doesNotCreateDuplicates() {
        SystemConfig config = new SystemConfig();

        // First call: uninitialized
        when(systemConfigService.isInitialized()).thenReturn(false);
        when(systemConfigService.getConfig()).thenReturn(config);
        when(systemConfigService.save(any())).thenReturn(config);

        startupService.onApplicationReady();

        // Second call: still uninitialized
        startupService.onApplicationReady();

        // Third call: now initialized
        when(systemConfigService.isInitialized()).thenReturn(true);
        startupService.onApplicationReady();

        // getConfig() was called exactly twice (once per uninitialized call)
        verify(systemConfigService, times(2)).getConfig();
        // save() was called exactly twice (once per uninitialized call)
        verify(systemConfigService, times(2)).save(any());
        // AP mode was started twice and stopped once
        verify(apModeService, times(2)).startApMode();
        verify(apModeService, times(1)).stopApMode();
    }
}
