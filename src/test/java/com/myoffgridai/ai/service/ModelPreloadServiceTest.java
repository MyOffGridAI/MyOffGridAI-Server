package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.OllamaChatRequest;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.common.exception.OllamaUnavailableException;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelPreloadServiceTest {

    @Mock private OllamaService ollamaService;
    @Mock private SystemConfigService systemConfigService;

    private ModelPreloadService preloadService;

    @BeforeEach
    void setUp() {
        preloadService = new ModelPreloadService(ollamaService, systemConfigService);
    }

    @Test
    void preloadModels_loadsEmbeddingAndChatModels() {
        AiSettingsDto settings = new AiSettingsDto("test-model", 0.7, 0.5, 5, 2048, 4096, 20);
        when(systemConfigService.getAiSettings()).thenReturn(settings);
        when(ollamaService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(ollamaService.chat(any(OllamaChatRequest.class)))
                .thenReturn(new OllamaChatResponse(new OllamaMessage("assistant", "hi"), true, null, null));

        preloadService.preloadModels();

        verify(ollamaService).embed("warmup");

        ArgumentCaptor<OllamaChatRequest> captor = ArgumentCaptor.forClass(OllamaChatRequest.class);
        verify(ollamaService).chat(captor.capture());
        OllamaChatRequest request = captor.getValue();
        assertThat(request.model()).isEqualTo("test-model");
        assertThat(request.keepAlive()).isEqualTo("24h");
        assertThat(request.options()).containsEntry("num_predict", 1);
    }

    @Test
    void preloadModels_embedFailure_doesNotPreventChatPreload() {
        AiSettingsDto settings = new AiSettingsDto("test-model", 0.7, 0.5, 5, 2048, 4096, 20);
        when(systemConfigService.getAiSettings()).thenReturn(settings);
        when(ollamaService.embed(anyString()))
                .thenThrow(new OllamaUnavailableException("Ollama down"));
        when(ollamaService.chat(any(OllamaChatRequest.class)))
                .thenReturn(new OllamaChatResponse(new OllamaMessage("assistant", "hi"), true, null, null));

        preloadService.preloadModels();

        verify(ollamaService).embed("warmup");
        verify(ollamaService).chat(any(OllamaChatRequest.class));
    }

    @Test
    void preloadModels_chatFailure_doesNotThrow() {
        AiSettingsDto settings = new AiSettingsDto("test-model", 0.7, 0.5, 5, 2048, 4096, 20);
        when(systemConfigService.getAiSettings()).thenReturn(settings);
        when(ollamaService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(ollamaService.chat(any(OllamaChatRequest.class)))
                .thenThrow(new OllamaUnavailableException("Ollama down"));

        assertThatCode(() -> preloadService.preloadModels()).doesNotThrowAnyException();
    }

    @Test
    void preloadModels_bothFail_doesNotThrow() {
        when(ollamaService.embed(anyString()))
                .thenThrow(new OllamaUnavailableException("Ollama down"));

        AiSettingsDto settings = new AiSettingsDto("test-model", 0.7, 0.5, 5, 2048, 4096, 20);
        when(systemConfigService.getAiSettings()).thenReturn(settings);
        when(ollamaService.chat(any(OllamaChatRequest.class)))
                .thenThrow(new OllamaUnavailableException("Still down"));

        assertThatCode(() -> preloadService.preloadModels()).doesNotThrowAnyException();
    }
}
