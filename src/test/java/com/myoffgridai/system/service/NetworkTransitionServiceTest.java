package com.myoffgridai.system.service;

import com.myoffgridai.system.model.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkTransitionServiceTest {

    @Mock
    private ApModeService apModeService;

    @Mock
    private SystemConfigService systemConfigService;

    private NetworkTransitionService networkTransitionService;

    @BeforeEach
    void setUp() {
        networkTransitionService = new NetworkTransitionService(
                apModeService, systemConfigService, true);
    }

    @Test
    void finalizeSetup_stopsApModeAndUpdatesConfig() {
        SystemConfig config = new SystemConfig();
        config.setApModeEnabled(true);
        when(systemConfigService.getConfig()).thenReturn(config);
        when(systemConfigService.save(any())).thenReturn(config);

        networkTransitionService.finalizeSetup();

        verify(apModeService).stopApMode();
        verify(systemConfigService).save(argThat(c ->
                !c.isApModeEnabled() && c.isWifiConfigured()));
    }

    @Test
    void finalizeSetup_setsWifiConfiguredTrue() {
        SystemConfig config = new SystemConfig();
        when(systemConfigService.getConfig()).thenReturn(config);
        when(systemConfigService.save(any())).thenReturn(config);

        networkTransitionService.finalizeSetup();

        verify(systemConfigService).save(argThat(SystemConfig::isWifiConfigured));
    }

    @Test
    void finalizeSetup_hasAsyncAnnotation() throws NoSuchMethodException {
        Method method = NetworkTransitionService.class.getMethod("finalizeSetup");
        assertTrue(method.isAnnotationPresent(Async.class));
    }
}
