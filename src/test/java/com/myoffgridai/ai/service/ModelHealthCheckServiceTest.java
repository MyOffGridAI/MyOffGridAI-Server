package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.OllamaModelInfo;
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

    @InjectMocks
    private ModelHealthCheckService healthCheckService;

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
