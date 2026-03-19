package com.myoffgridai.knowledge.service;

import com.myoffgridai.knowledge.dto.KnowledgeSearchResultDto;
import com.myoffgridai.knowledge.model.KnowledgeChunk;
import com.myoffgridai.knowledge.model.KnowledgeDocument;
import com.myoffgridai.knowledge.repository.KnowledgeChunkRepository;
import com.myoffgridai.knowledge.repository.KnowledgeDocumentRepository;
import com.myoffgridai.memory.model.VectorDocument;
import com.myoffgridai.memory.repository.VectorDocumentRepository;
import com.myoffgridai.memory.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    @Mock private VectorDocumentRepository vectorDocumentRepository;
    @Mock private KnowledgeChunkRepository chunkRepository;
    @Mock private KnowledgeDocumentRepository documentRepository;
    @Mock private EmbeddingService embeddingService;

    private SemanticSearchService searchService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        searchService = new SemanticSearchService(
                vectorDocumentRepository, chunkRepository, documentRepository, embeddingService);
        userId = UUID.randomUUID();
    }

    @Test
    void search_withResults_returnsEnrichedDtos() {
        UUID chunkId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(docId);
        doc.setFilename("guide.pdf");
        doc.setDisplayName("Solar Guide");

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(chunkId);
        chunk.setDocument(doc);
        chunk.setChunkIndex(0);
        chunk.setContent("Solar panels need sunlight");
        chunk.setPageNumber(3);

        float[] embedding = new float[]{0.1f, 0.2f};
        VectorDocument vd = new VectorDocument();
        vd.setSourceId(chunkId);
        vd.setEmbedding(embedding);
        vd.setContent("Solar panels need sunlight");

        when(embeddingService.embed(anyString())).thenReturn(embedding);
        when(embeddingService.cosineSimilarity(any(), any())).thenReturn(0.92f);
        when(vectorDocumentRepository.findMostSimilar(eq(userId), eq("KNOWLEDGE_CHUNK"), anyString(), anyInt()))
                .thenReturn(List.of(vd));
        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));

        List<KnowledgeSearchResultDto> results = searchService.search(userId, "solar energy", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).documentName()).isEqualTo("Solar Guide");
        assertThat(results.get(0).content()).isEqualTo("Solar panels need sunlight");
        assertThat(results.get(0).pageNumber()).isEqualTo(3);
        assertThat(results.get(0).similarityScore()).isEqualTo(0.92f);
    }

    @Test
    void search_noResults_returnsEmpty() {
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(vectorDocumentRepository.findMostSimilar(eq(userId), eq("KNOWLEDGE_CHUNK"), anyString(), anyInt()))
                .thenReturn(List.of());

        List<KnowledgeSearchResultDto> results = searchService.search(userId, "nothing", 5);

        assertThat(results).isEmpty();
    }

    @Test
    void search_chunkNotFound_skipsResult() {
        UUID chunkId = UUID.randomUUID();
        VectorDocument vd = new VectorDocument();
        vd.setSourceId(chunkId);
        vd.setEmbedding(new float[]{0.1f});

        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(vectorDocumentRepository.findMostSimilar(eq(userId), eq("KNOWLEDGE_CHUNK"), anyString(), anyInt()))
                .thenReturn(List.of(vd));
        when(chunkRepository.findById(chunkId)).thenReturn(Optional.empty());

        List<KnowledgeSearchResultDto> results = searchService.search(userId, "test", 5);

        assertThat(results).isEmpty();
    }

    @Test
    void search_nullSourceId_skipsResult() {
        VectorDocument vd = new VectorDocument();
        vd.setSourceId(null);

        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(vectorDocumentRepository.findMostSimilar(eq(userId), eq("KNOWLEDGE_CHUNK"), anyString(), anyInt()))
                .thenReturn(List.of(vd));

        List<KnowledgeSearchResultDto> results = searchService.search(userId, "test", 5);

        assertThat(results).isEmpty();
    }

    @Test
    void search_usesFilenameWhenNoDisplayName() {
        UUID chunkId = UUID.randomUUID();

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(UUID.randomUUID());
        doc.setFilename("manual.pdf");
        doc.setDisplayName(null);

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(chunkId);
        chunk.setDocument(doc);
        chunk.setChunkIndex(0);
        chunk.setContent("content");

        VectorDocument vd = new VectorDocument();
        vd.setSourceId(chunkId);
        vd.setEmbedding(new float[]{0.1f});

        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(embeddingService.cosineSimilarity(any(), any())).thenReturn(0.8f);
        when(vectorDocumentRepository.findMostSimilar(eq(userId), eq("KNOWLEDGE_CHUNK"), anyString(), anyInt()))
                .thenReturn(List.of(vd));
        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));

        List<KnowledgeSearchResultDto> results = searchService.search(userId, "test", 5);

        assertThat(results.get(0).documentName()).isEqualTo("manual.pdf");
    }

    @Test
    void searchForRagContext_returnsAttributedSnippets() {
        UUID chunkId = UUID.randomUUID();

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(UUID.randomUUID());
        doc.setFilename("guide.pdf");
        doc.setDisplayName("My Guide");

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(chunkId);
        chunk.setDocument(doc);
        chunk.setChunkIndex(0);
        chunk.setContent("Important info");

        VectorDocument vd = new VectorDocument();
        vd.setSourceId(chunkId);
        vd.setEmbedding(new float[]{0.1f});

        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(embeddingService.cosineSimilarity(any(), any())).thenReturn(0.9f);
        when(vectorDocumentRepository.findMostSimilar(eq(userId), eq("KNOWLEDGE_CHUNK"), anyString(), anyInt()))
                .thenReturn(List.of(vd));
        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));

        List<String> snippets = searchService.searchForRagContext(userId, "query", 5);

        assertThat(snippets).hasSize(1);
        assertThat(snippets.get(0)).isEqualTo("[My Guide]: Important info");
    }
}
