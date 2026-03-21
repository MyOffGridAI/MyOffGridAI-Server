package com.myoffgridai.knowledge.service;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.common.exception.UnsupportedFileTypeException;
import com.myoffgridai.knowledge.dto.DocumentContentDto;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.model.DocumentStatus;
import com.myoffgridai.knowledge.model.KnowledgeChunk;
import com.myoffgridai.knowledge.model.KnowledgeDocument;
import com.myoffgridai.knowledge.repository.KnowledgeChunkRepository;
import com.myoffgridai.knowledge.repository.KnowledgeDocumentRepository;
import com.myoffgridai.memory.model.VectorSourceType;
import com.myoffgridai.memory.repository.VectorDocumentRepository;
import com.myoffgridai.memory.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.AfterEach;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock private KnowledgeDocumentRepository documentRepository;
    @Mock private KnowledgeChunkRepository chunkRepository;
    @Mock private VectorDocumentRepository vectorDocumentRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private IngestionService ingestionService;
    @Mock private OcrService ocrService;
    @Mock private ChunkingService chunkingService;
    @Mock private EmbeddingService embeddingService;
    @Mock private ApplicationContext applicationContext;
    @Mock private UserRepository userRepository;

    private KnowledgeService knowledgeService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        knowledgeService = new KnowledgeService(
                documentRepository, chunkRepository, vectorDocumentRepository,
                fileStorageService, ingestionService, ocrService,
                chunkingService, embeddingService, applicationContext, userRepository);
        userId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void upload_validFile_createsDocumentInPendingStatus() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "content".getBytes());
        when(fileStorageService.store(eq(userId), any(), eq("test.txt")))
                .thenReturn("/path/to/file");
        when(documentRepository.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> {
                    KnowledgeDocument doc = invocation.getArgument(0);
                    doc.setId(UUID.randomUUID());
                    return doc;
                });

        KnowledgeDocumentDto dto = knowledgeService.upload(userId, file);

        assertThat(dto.filename()).isEqualTo("test.txt");
        assertThat(dto.mimeType()).isEqualTo("text/plain");
        assertThat(dto.status()).isEqualTo(DocumentStatus.PENDING);
        verify(documentRepository).save(any(KnowledgeDocument.class));
    }

    @Test
    void upload_unsupportedMimeType_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.exe", "application/x-msdownload", "content".getBytes());

        assertThatThrownBy(() -> knowledgeService.upload(userId, file))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }

    @Test
    void upload_nullMimeType_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.bin", null, "content".getBytes());

        assertThatThrownBy(() -> knowledgeService.upload(userId, file))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }

    @Test
    void listDocuments_returnsPagedResults() {
        KnowledgeDocument doc = createTestDocument();
        Page<KnowledgeDocument> page = new PageImpl<>(List.of(doc));
        when(documentRepository.findByUserIdOrderByUploadedAtDesc(eq(userId), any()))
                .thenReturn(page);

        Page<KnowledgeDocumentDto> result = knowledgeService.listDocuments(userId, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).filename()).isEqualTo("test.pdf");
    }

    @Test
    void getDocument_existing_returnsDto() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(docId);
        when(documentRepository.findByIdAndUserId(docId, userId))
                .thenReturn(Optional.of(doc));

        KnowledgeDocumentDto dto = knowledgeService.getDocument(docId, userId);

        assertThat(dto.id()).isEqualTo(docId);
        assertThat(dto.filename()).isEqualTo("test.pdf");
    }

    @Test
    void getDocument_notFound_throwsEntityNotFound() {
        UUID docId = UUID.randomUUID();
        when(documentRepository.findByIdAndUserId(docId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeService.getDocument(docId, userId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateDisplayName_updatesSuccessfully() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(docId);
        when(documentRepository.findByIdAndUserId(docId, userId))
                .thenReturn(Optional.of(doc));
        when(documentRepository.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeDocumentDto dto = knowledgeService.updateDisplayName(docId, userId, "My Guide");

        assertThat(dto.displayName()).isEqualTo("My Guide");
        verify(documentRepository).save(any());
    }

    @Test
    void deleteDocument_deletesDocChunksVectorsFile() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(docId);
        when(documentRepository.findByIdAndUserId(docId, userId))
                .thenReturn(Optional.of(doc));

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(UUID.randomUUID());
        when(chunkRepository.findByDocumentIdOrderByChunkIndexAsc(docId))
                .thenReturn(List.of(chunk));

        knowledgeService.deleteDocument(docId, userId);

        verify(vectorDocumentRepository).deleteBySourceIdAndSourceType(
                chunk.getId(), VectorSourceType.KNOWLEDGE_CHUNK);
        verify(chunkRepository).deleteByDocumentId(docId);
        verify(fileStorageService).delete(doc.getStoragePath());
        verify(documentRepository).delete(doc);
    }

    @Test
    void retryProcessing_failedDocument_reQueues() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(docId);
        doc.setStatus(DocumentStatus.FAILED);
        doc.setErrorMessage("previous error");
        when(documentRepository.findByIdAndUserId(docId, userId))
                .thenReturn(Optional.of(doc));
        when(chunkRepository.findByDocumentIdOrderByChunkIndexAsc(docId))
                .thenReturn(List.of());
        when(documentRepository.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeDocumentDto dto = knowledgeService.retryProcessing(docId, userId);

        assertThat(dto.status()).isEqualTo(DocumentStatus.PENDING);
        assertThat(dto.errorMessage()).isNull();
    }

    @Test
    void retryProcessing_nonFailedDocument_throwsIllegalArgument() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(docId);
        doc.setStatus(DocumentStatus.READY);
        when(documentRepository.findByIdAndUserId(docId, userId))
                .thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> knowledgeService.retryProcessing(docId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only FAILED documents");
    }

    @Test
    void getChunks_returnsOrderedChunks() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(docId);
        when(documentRepository.findByIdAndUserId(docId, userId))
                .thenReturn(Optional.of(doc));

        KnowledgeChunk chunk1 = new KnowledgeChunk();
        chunk1.setChunkIndex(0);
        chunk1.setContent("chunk 0");
        KnowledgeChunk chunk2 = new KnowledgeChunk();
        chunk2.setChunkIndex(1);
        chunk2.setContent("chunk 1");
        when(chunkRepository.findByDocumentIdOrderByChunkIndexAsc(docId))
                .thenReturn(List.of(chunk1, chunk2));

        List<KnowledgeChunk> chunks = knowledgeService.getChunks(docId, userId);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getChunkIndex()).isEqualTo(0);
    }

    @Test
    void toDto_mapsAllFieldsIncludingHasContentAndEditable() {
        KnowledgeDocument doc = createTestDocument();
        doc.setId(UUID.randomUUID());
        doc.setDisplayName("My Doc");
        doc.setChunkCount(10);
        doc.setStatus(DocumentStatus.READY);
        doc.setContent("[{\"insert\":\"some content\\n\"}]");
        doc.setMimeType("text/plain");

        KnowledgeDocumentDto dto = knowledgeService.toDto(doc, userId);

        assertThat(dto.id()).isEqualTo(doc.getId());
        assertThat(dto.filename()).isEqualTo(doc.getFilename());
        assertThat(dto.displayName()).isEqualTo("My Doc");
        assertThat(dto.mimeType()).isEqualTo(doc.getMimeType());
        assertThat(dto.fileSizeBytes()).isEqualTo(doc.getFileSizeBytes());
        assertThat(dto.status()).isEqualTo(DocumentStatus.READY);
        assertThat(dto.chunkCount()).isEqualTo(10);
        assertThat(dto.hasContent()).isTrue();
        assertThat(dto.editable()).isTrue();
        assertThat(dto.isOwner()).isTrue();
        assertThat(dto.isShared()).isFalse();
        assertThat(dto.ownerDisplayName()).isNull();
    }

    @Test
    void toDto_noContent_hasContentFalse() {
        KnowledgeDocument doc = createTestDocument();
        doc.setId(UUID.randomUUID());

        KnowledgeDocumentDto dto = knowledgeService.toDto(doc, userId);

        assertThat(dto.hasContent()).isFalse();
    }

    @Test
    void toDto_nonEditableMimeType_editableFalse() {
        KnowledgeDocument doc = createTestDocument();
        doc.setId(UUID.randomUUID());
        doc.setMimeType("application/pdf");

        KnowledgeDocumentDto dto = knowledgeService.toDto(doc, userId);

        assertThat(dto.editable()).isFalse();
    }

    @Test
    void toDto_nonOwner_editableFalse_ownerDisplayNameSet() {
        UUID otherUserId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(UUID.randomUUID());
        doc.setUserId(otherUserId);
        doc.setMimeType("text/plain");
        doc.setShared(true);

        User otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setDisplayName("Other User");
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

        KnowledgeDocumentDto dto = knowledgeService.toDto(doc, userId);

        assertThat(dto.isOwner()).isFalse();
        assertThat(dto.editable()).isFalse();
        assertThat(dto.ownerDisplayName()).isEqualTo("Other User");
        assertThat(dto.isShared()).isTrue();
    }

    @Test
    void getDocumentContent_returnsContentDto() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(docId);
        doc.setDisplayName("My Guide");
        doc.setContent("[{\"insert\":\"hello\\n\"}]");
        doc.setMimeType("text/plain");
        when(documentRepository.findByIdAndUserId(docId, userId))
                .thenReturn(Optional.of(doc));

        DocumentContentDto dto = knowledgeService.getDocumentContent(docId, userId);

        assertThat(dto.documentId()).isEqualTo(docId);
        assertThat(dto.title()).isEqualTo("My Guide");
        assertThat(dto.content()).isEqualTo("[{\"insert\":\"hello\\n\"}]");
        assertThat(dto.editable()).isTrue();
    }

    @Test
    void getDocumentContent_noDisplayName_usesFilename() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(docId);
        doc.setContent("[{\"insert\":\"text\\n\"}]");
        when(documentRepository.findByIdAndUserId(docId, userId))
                .thenReturn(Optional.of(doc));

        DocumentContentDto dto = knowledgeService.getDocumentContent(docId, userId);

        assertThat(dto.title()).isEqualTo("test.pdf");
    }

    @Test
    void getDocumentForDownload_returnsEntity() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(docId);
        when(documentRepository.findByIdAndUserId(docId, userId))
                .thenReturn(Optional.of(doc));

        KnowledgeDocument result = knowledgeService.getDocumentForDownload(docId, userId);

        assertThat(result.getId()).isEqualTo(docId);
    }

    @Test
    void createFromEditor_createsDocumentWithQuillMimeType() {
        String deltaJson = "[{\"insert\":\"New doc content\\n\"}]";
        when(fileStorageService.storeBytes(eq(userId), any(byte[].class), anyString()))
                .thenReturn("/path/to/file");
        when(documentRepository.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> {
                    KnowledgeDocument doc = invocation.getArgument(0);
                    doc.setId(UUID.randomUUID());
                    return doc;
                });

        KnowledgeDocumentDto dto = knowledgeService.createFromEditor(userId, "My New Doc", deltaJson);

        assertThat(dto.status()).isEqualTo(DocumentStatus.PENDING);
        assertThat(dto.mimeType()).isEqualTo("application/x-quill-delta");
        verify(fileStorageService).storeBytes(eq(userId), any(byte[].class), anyString());
        verify(documentRepository).save(any(KnowledgeDocument.class));
    }

    @Test
    void updateContent_clearsChunksAndReProcesses() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(docId);
        doc.setMimeType("text/plain");
        doc.setContent("[{\"insert\":\"old\\n\"}]");
        doc.setStatus(DocumentStatus.READY);
        doc.setChunkCount(5);
        when(documentRepository.findByIdAndUserId(docId, userId))
                .thenReturn(Optional.of(doc));

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(UUID.randomUUID());
        when(chunkRepository.findByDocumentIdOrderByChunkIndexAsc(docId))
                .thenReturn(List.of(chunk));

        when(documentRepository.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String newDelta = "[{\"insert\":\"updated content\\n\"}]";
        KnowledgeDocumentDto dto = knowledgeService.updateContent(docId, userId, newDelta);

        assertThat(dto.status()).isEqualTo(DocumentStatus.PENDING);
        assertThat(dto.chunkCount()).isEqualTo(0);
        verify(vectorDocumentRepository).deleteBySourceIdAndSourceType(
                chunk.getId(), VectorSourceType.KNOWLEDGE_CHUNK);
        verify(chunkRepository).deleteByDocumentId(docId);
        verify(documentRepository).save(any(KnowledgeDocument.class));
    }

    @Test
    void toggleSharing_ownerCanShare() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(docId);
        when(documentRepository.findByIdAndUserId(docId, userId))
                .thenReturn(Optional.of(doc));
        when(documentRepository.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeDocumentDto dto = knowledgeService.updateSharing(docId, userId, true);

        assertThat(dto.isShared()).isTrue();
        verify(documentRepository).save(any());
    }

    @Test
    void toggleSharing_nonOwner_throwsEntityNotFound() {
        UUID docId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(documentRepository.findByIdAndUserId(docId, otherUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeService.updateSharing(docId, otherUserId, true))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getDocument_sharedDocByNonOwner_returnsDto() {
        UUID docId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(docId);
        doc.setShared(true);
        // Not found by owner query, but found by shared query
        when(documentRepository.findByIdAndUserId(docId, otherUserId))
                .thenReturn(Optional.empty());
        when(documentRepository.findByIdAndIsSharedTrue(docId))
                .thenReturn(Optional.of(doc));
        User owner = new User();
        owner.setId(userId);
        owner.setDisplayName("Doc Owner");
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));

        KnowledgeDocumentDto dto = knowledgeService.getDocument(docId, otherUserId);

        assertThat(dto.id()).isEqualTo(docId);
        assertThat(dto.isOwner()).isFalse();
        assertThat(dto.ownerDisplayName()).isEqualTo("Doc Owner");
    }

    @Test
    void getDocument_unsharedDocByNonOwner_throwsEntityNotFound() {
        UUID docId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(documentRepository.findByIdAndUserId(docId, otherUserId))
                .thenReturn(Optional.empty());
        when(documentRepository.findByIdAndIsSharedTrue(docId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeService.getDocument(docId, otherUserId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void listDocuments_scopeShared_returnsOnlySharedFromOthers() {
        UUID otherUserId = UUID.randomUUID();
        KnowledgeDocument sharedDoc = createTestDocument();
        sharedDoc.setId(UUID.randomUUID());
        sharedDoc.setUserId(otherUserId);
        sharedDoc.setShared(true);

        User otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setDisplayName("Other");
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

        Page<KnowledgeDocument> page = new PageImpl<>(List.of(sharedDoc));
        when(documentRepository.findByIsSharedTrueAndUserIdNotOrderByUploadedAtDesc(eq(userId), any()))
                .thenReturn(page);

        Page<KnowledgeDocumentDto> result = knowledgeService.listDocuments(userId, "SHARED", PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isOwner()).isFalse();
        assertThat(result.getContent().get(0).isShared()).isTrue();
    }

    @Test
    void getDocumentContent_sharedDoc_editableFalse() {
        UUID docId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        KnowledgeDocument doc = createTestDocument();
        doc.setId(docId);
        doc.setMimeType("text/plain");
        doc.setContent("[{\"insert\":\"hello\\n\"}]");
        doc.setShared(true);
        // Non-owner access via shared
        when(documentRepository.findByIdAndUserId(docId, otherUserId))
                .thenReturn(Optional.empty());
        when(documentRepository.findByIdAndIsSharedTrue(docId))
                .thenReturn(Optional.of(doc));

        DocumentContentDto dto = knowledgeService.getDocumentContent(docId, otherUserId);

        assertThat(dto.editable()).isFalse();
    }

    @Test
    void deleteDocument_nonOwner_throwsEntityNotFound() {
        UUID docId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(documentRepository.findByIdAndUserId(docId, otherUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeService.deleteDocument(docId, otherUserId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private KnowledgeDocument createTestDocument() {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setUserId(userId);
        doc.setFilename("test.pdf");
        doc.setMimeType("application/pdf");
        doc.setStoragePath("/path/to/test.pdf");
        doc.setFileSizeBytes(1024);
        doc.setStatus(DocumentStatus.PENDING);
        return doc;
    }
}
