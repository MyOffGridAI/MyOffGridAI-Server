package com.myoffgridai.system.service;

import com.myoffgridai.system.model.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

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
}
