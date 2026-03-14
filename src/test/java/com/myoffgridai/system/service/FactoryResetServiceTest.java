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
class FactoryResetServiceTest {

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private ApModeService apModeService;

    private FactoryResetService factoryResetService;

    @BeforeEach
    void setUp() {
        factoryResetService = new FactoryResetService(systemConfigService, apModeService);
    }

    @Test
    void performReset_resetsSystemConfigToDefaults() {
        SystemConfig config = new SystemConfig();
        config.setInitialized(true);
        config.setInstanceName("Test Instance");
        config.setFortressEnabled(true);
        when(systemConfigService.getConfig()).thenReturn(config);
        when(systemConfigService.save(any())).thenReturn(config);

        factoryResetService.performReset();

        verify(systemConfigService, atLeastOnce()).save(argThat(c ->
                !c.isInitialized()
                        && c.getInstanceName() == null
                        && !c.isFortressEnabled()));
    }

    @Test
    void performReset_restartsApMode() {
        SystemConfig config = new SystemConfig();
        when(systemConfigService.getConfig()).thenReturn(config);
        when(systemConfigService.save(any())).thenReturn(config);

        factoryResetService.performReset();

        verify(apModeService).startApMode();
    }

    @Test
    void performReset_hasAsyncAnnotation() throws NoSuchMethodException {
        Method method = FactoryResetService.class.getMethod("performReset");
        assertTrue(method.isAnnotationPresent(Async.class));
    }

    @Test
    void performUsbReset_resetsSystemConfig() {
        SystemConfig config = new SystemConfig();
        config.setInitialized(true);
        when(systemConfigService.getConfig()).thenReturn(config);
        when(systemConfigService.save(any())).thenReturn(config);

        factoryResetService.performUsbReset();

        verify(systemConfigService, atLeastOnce()).save(argThat(c -> !c.isInitialized()));
        verify(apModeService).startApMode();
    }

    @Test
    void performUsbReset_doesNotRequireAuth() {
        // performUsbReset is a plain public method — no @PreAuthorize
        SystemConfig config = new SystemConfig();
        when(systemConfigService.getConfig()).thenReturn(config);
        when(systemConfigService.save(any())).thenReturn(config);

        assertDoesNotThrow(() -> factoryResetService.performUsbReset());
    }
}
