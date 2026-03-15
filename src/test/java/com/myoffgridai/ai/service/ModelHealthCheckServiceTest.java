package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.OllamaModelInfo;
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

@ExtendWith(MockitoExtension.class)
class ModelHealthCheckServiceTest {

    @Mock
    private OllamaService ollamaService;

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private ModelHealthCheckService healthCheckService;

    @BeforeEach
    void setUp() {
        lenient().when(systemConfigService.getAiSettings())
                .thenReturn(new AiSettingsDto("hf.co/Qwen/Qwen3-32B-GGUF:Q4_K_M", 0.7, 0.45, 5, 2048));
    }

    @Test
    void checkOllamaOnStartup_ollamaUnavailable_doesNotThrow() {
        when(ollamaService.isAvailable()).thenReturn(false);

        assertDoesNotThrow(() -> healthCheckService.checkOllamaOnStartup());
        verify(ollamaService, never()).listModels();
    }

    @Test
    void checkOllamaOnStartup_ollamaAvailable_listsModels() {
        when(ollamaService.isAvailable()).thenReturn(true);
        when(ollamaService.listModels()).thenReturn(List.of(
                new OllamaModelInfo("qwen3:32b", 17_000_000_000L, Instant.now())
        ));

        assertDoesNotThrow(() -> healthCheckService.checkOllamaOnStartup());
        verify(ollamaService).listModels();
    }

    @Test
    void checkOllamaOnStartup_modelNotFound_doesNotThrow() {
        when(ollamaService.isAvailable()).thenReturn(true);
        when(ollamaService.listModels()).thenReturn(List.of(
                new OllamaModelInfo("llama3:8b", 4_000_000_000L, Instant.now())
        ));

        assertDoesNotThrow(() -> healthCheckService.checkOllamaOnStartup());
    }

    @Test
    void checkOllamaOnStartup_listModelsFails_doesNotThrow() {
        when(ollamaService.isAvailable()).thenReturn(true);
        when(ollamaService.listModels()).thenThrow(new RuntimeException("list failed"));

        assertDoesNotThrow(() -> healthCheckService.checkOllamaOnStartup());
    }
}
