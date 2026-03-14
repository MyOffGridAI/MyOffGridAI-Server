package com.myoffgridai.memory.service;

import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.common.exception.EmbeddingException;
import com.myoffgridai.common.exception.OllamaUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private OllamaService ollamaService;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(ollamaService);
    }

    @Test
    void embed_delegatesToOllamaService() {
        float[] expected = {0.1f, 0.2f, 0.3f};
        when(ollamaService.embed("test text")).thenReturn(expected);

        float[] result = embeddingService.embed("test text");
        assertArrayEquals(expected, result);
        verify(ollamaService).embed("test text");
    }

    @Test
    void embed_throwsEmbeddingException_whenOllamaUnavailable() {
        when(ollamaService.embed(anyString()))
                .thenThrow(new OllamaUnavailableException("unavailable"));

        assertThrows(EmbeddingException.class, () -> embeddingService.embed("test"));
    }

    @Test
    void embedAndFormat_returnsCorrectFormat() {
        float[] embedding = {0.1f, 0.2f, 0.3f};
        when(ollamaService.embed("test")).thenReturn(embedding);

        String result = embeddingService.embedAndFormat("test");
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("0.1"));
        assertTrue(result.contains("0.2"));
        assertTrue(result.contains("0.3"));
    }

    @Test
    void embedBatch_delegatesToOllamaService() {
        float[] e1 = {0.1f};
        float[] e2 = {0.2f};
        when(ollamaService.embedBatch(List.of("a", "b"))).thenReturn(List.of(e1, e2));

        List<float[]> results = embeddingService.embedBatch(List.of("a", "b"));
        assertEquals(2, results.size());
    }

    @Test
    void cosineSimilarity_identicalVectors_returnsOne() {
        float[] a = {1.0f, 0.0f, 0.0f};
        float result = embeddingService.cosineSimilarity(a, a);
        assertEquals(1.0f, result, 0.001f);
    }

    @Test
    void cosineSimilarity_orthogonalVectors_returnsZero() {
        float[] a = {1.0f, 0.0f};
        float[] b = {0.0f, 1.0f};
        float result = embeddingService.cosineSimilarity(a, b);
        assertEquals(0.0f, result, 0.001f);
    }

    @Test
    void cosineSimilarity_nullInput_returnsZero() {
        float result = embeddingService.cosineSimilarity(null, new float[]{1.0f});
        assertEquals(0.0f, result);
    }

    @Test
    void cosineSimilarity_differentLengths_returnsZero() {
        float result = embeddingService.cosineSimilarity(new float[]{1.0f}, new float[]{1.0f, 2.0f});
        assertEquals(0.0f, result);
    }

    @Test
    void formatEmbedding_formatsCorrectly() {
        float[] embedding = {0.1f, 0.2f, 0.3f};
        String result = EmbeddingService.formatEmbedding(embedding);
        assertEquals("[0.1,0.2,0.3]", result);
    }
}
