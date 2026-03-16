package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ModelHealthCheckService}.
 *
 * <p>Validates inference provider health check at startup, verifying
 * graceful handling of unavailable providers and model listing failures.</p>
 */
@ExtendWith(MockitoExtension.class)
class ModelHealthCheckServiceTest {

    @Mock
    private InferenceService inferenceService;

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private ModelHealthCheckService healthCheckService;

    @BeforeEach
    void setUp() {
        lenient().when(systemConfigService.getAiSettings())
                .thenReturn(new AiSettingsDto("test-model", 0.7, 0.45, 5, 2048, 4096, 20));
    }

    @Test
    void checkInferenceProviderOnStartup_providerUnavailable_doesNotThrow() {
        when(inferenceService.isAvailable()).thenReturn(false);

        assertDoesNotThrow(() -> healthCheckService.checkInferenceProviderOnStartup());
        verify(inferenceService, never()).listModels();
    }

    @Test
    void checkInferenceProviderOnStartup_providerAvailable_listsModels() {
        when(inferenceService.isAvailable()).thenReturn(true);
        when(inferenceService.listModels()).thenReturn(List.of(
                new InferenceModelInfo("qwen3:32b", "qwen3:32b", 17_000_000_000L, "gguf", Instant.now())
        ));

        assertDoesNotThrow(() -> healthCheckService.checkInferenceProviderOnStartup());
        verify(inferenceService).listModels();
    }

    @Test
    void checkInferenceProviderOnStartup_modelNotFound_doesNotThrow() {
        when(inferenceService.isAvailable()).thenReturn(true);
        when(inferenceService.listModels()).thenReturn(List.of(
                new InferenceModelInfo("llama3:8b", "llama3:8b", 4_000_000_000L, "gguf", Instant.now())
        ));

        assertDoesNotThrow(() -> healthCheckService.checkInferenceProviderOnStartup());
    }

    @Test
    void checkInferenceProviderOnStartup_listModelsFails_doesNotThrow() {
        when(inferenceService.isAvailable()).thenReturn(true);
        when(inferenceService.listModels()).thenThrow(new RuntimeException("list failed"));

        assertDoesNotThrow(() -> healthCheckService.checkInferenceProviderOnStartup());
    }
}
