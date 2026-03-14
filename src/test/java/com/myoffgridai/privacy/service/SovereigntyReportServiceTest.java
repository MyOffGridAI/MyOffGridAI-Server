package com.myoffgridai.privacy.service;

import com.myoffgridai.ai.repository.ConversationRepository;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.knowledge.repository.KnowledgeDocumentRepository;
import com.myoffgridai.memory.repository.MemoryRepository;
import com.myoffgridai.privacy.dto.AuditSummary;
import com.myoffgridai.privacy.dto.DataInventory;
import com.myoffgridai.privacy.dto.FortressStatus;
import com.myoffgridai.privacy.dto.SovereigntyReport;
import com.myoffgridai.privacy.model.AuditOutcome;
import com.myoffgridai.proactive.repository.InsightRepository;
import com.myoffgridai.sensors.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SovereigntyReportService}.
 */
@ExtendWith(MockitoExtension.class)
class SovereigntyReportServiceTest {

    @Mock
    private FortressService fortressService;

    @Mock
    private AuditService auditService;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private InsightRepository insightRepository;

    private SovereigntyReportService sovereigntyReportService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        sovereigntyReportService = new SovereigntyReportService(
                fortressService,
                auditService,
                conversationRepository,
                messageRepository,
                memoryRepository,
                knowledgeDocumentRepository,
                sensorRepository,
                insightRepository
        );
        userId = UUID.randomUUID();
    }

    @Test
    void generateReport_assemblesFullReport() {
        FortressStatus fortressStatus = new FortressStatus(true, Instant.now(), "admin", true);
        when(fortressService.getFortressStatus()).thenReturn(fortressStatus);
        when(conversationRepository.countByUserId(userId)).thenReturn(5L);
        when(messageRepository.countByUserId(userId)).thenReturn(100L);
        when(memoryRepository.countByUserId(userId)).thenReturn(10L);
        when(knowledgeDocumentRepository.countByUserId(userId)).thenReturn(3L);
        when(sensorRepository.countByUserId(userId)).thenReturn(2L);
        when(insightRepository.countByUserId(userId)).thenReturn(7L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.SUCCESS), any(Instant.class), any(Instant.class)))
                .thenReturn(50L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.FAILURE), any(Instant.class), any(Instant.class)))
                .thenReturn(2L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.DENIED), any(Instant.class), any(Instant.class)))
                .thenReturn(1L);

        SovereigntyReport report = sovereigntyReportService.generateReport(userId);

        assertNotNull(report);
        assertNotNull(report.generatedAt());
        assertNotNull(report.fortressStatus());
        assertNotNull(report.outboundTrafficVerification());
        assertNotNull(report.dataInventory());
        assertNotNull(report.auditSummary());
        assertNotNull(report.encryptionStatus());
        assertNotNull(report.telemetryStatus());
        assertNotNull(report.lastVerifiedAt());
        assertEquals(fortressStatus, report.fortressStatus());
    }

    @Test
    void generateReport_fortressEnabled_blocksTraffic() {
        FortressStatus fortressStatus = new FortressStatus(true, Instant.now(), "admin", true);
        when(fortressService.getFortressStatus()).thenReturn(fortressStatus);
        when(conversationRepository.countByUserId(userId)).thenReturn(0L);
        when(messageRepository.countByUserId(userId)).thenReturn(0L);
        when(memoryRepository.countByUserId(userId)).thenReturn(0L);
        when(knowledgeDocumentRepository.countByUserId(userId)).thenReturn(0L);
        when(sensorRepository.countByUserId(userId)).thenReturn(0L);
        when(insightRepository.countByUserId(userId)).thenReturn(0L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.SUCCESS), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.FAILURE), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.DENIED), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);

        SovereigntyReport report = sovereigntyReportService.generateReport(userId);

        assertTrue(report.outboundTrafficVerification().contains("BLOCKED"));
    }

    @Test
    void generateReport_fortressDisabled_allowsTraffic() {
        FortressStatus fortressStatus = new FortressStatus(false, null, null, true);
        when(fortressService.getFortressStatus()).thenReturn(fortressStatus);
        when(conversationRepository.countByUserId(userId)).thenReturn(0L);
        when(messageRepository.countByUserId(userId)).thenReturn(0L);
        when(memoryRepository.countByUserId(userId)).thenReturn(0L);
        when(knowledgeDocumentRepository.countByUserId(userId)).thenReturn(0L);
        when(sensorRepository.countByUserId(userId)).thenReturn(0L);
        when(insightRepository.countByUserId(userId)).thenReturn(0L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.SUCCESS), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.FAILURE), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.DENIED), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);

        SovereigntyReport report = sovereigntyReportService.generateReport(userId);

        assertTrue(report.outboundTrafficVerification().contains("ALLOWED"));
    }

    @Test
    void generateReport_includesDataInventory() {
        FortressStatus fortressStatus = new FortressStatus(false, null, null, true);
        when(fortressService.getFortressStatus()).thenReturn(fortressStatus);
        when(conversationRepository.countByUserId(userId)).thenReturn(12L);
        when(messageRepository.countByUserId(userId)).thenReturn(200L);
        when(memoryRepository.countByUserId(userId)).thenReturn(30L);
        when(knowledgeDocumentRepository.countByUserId(userId)).thenReturn(8L);
        when(sensorRepository.countByUserId(userId)).thenReturn(4L);
        when(insightRepository.countByUserId(userId)).thenReturn(15L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.SUCCESS), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.FAILURE), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.DENIED), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);

        SovereigntyReport report = sovereigntyReportService.generateReport(userId);

        DataInventory inventory = report.dataInventory();
        assertEquals(12L, inventory.conversationCount());
        assertEquals(200L, inventory.messageCount());
        assertEquals(30L, inventory.memoryCount());
        assertEquals(8L, inventory.knowledgeDocumentCount());
        assertEquals(4L, inventory.sensorCount());
        assertEquals(15L, inventory.insightCount());
    }

    @Test
    void generateReport_includesAuditSummary() {
        FortressStatus fortressStatus = new FortressStatus(true, Instant.now(), "admin", true);
        when(fortressService.getFortressStatus()).thenReturn(fortressStatus);
        when(conversationRepository.countByUserId(userId)).thenReturn(0L);
        when(messageRepository.countByUserId(userId)).thenReturn(0L);
        when(memoryRepository.countByUserId(userId)).thenReturn(0L);
        when(knowledgeDocumentRepository.countByUserId(userId)).thenReturn(0L);
        when(sensorRepository.countByUserId(userId)).thenReturn(0L);
        when(insightRepository.countByUserId(userId)).thenReturn(0L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.SUCCESS), any(Instant.class), any(Instant.class)))
                .thenReturn(99L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.FAILURE), any(Instant.class), any(Instant.class)))
                .thenReturn(5L);
        when(auditService.countByOutcomeBetween(eq(AuditOutcome.DENIED), any(Instant.class), any(Instant.class)))
                .thenReturn(3L);

        SovereigntyReport report = sovereigntyReportService.generateReport(userId);

        AuditSummary summary = report.auditSummary();
        assertEquals(99L, summary.successCount());
        assertEquals(5L, summary.failureCount());
        assertEquals(3L, summary.deniedCount());
        assertNotNull(summary.windowStart());
        assertNotNull(summary.windowEnd());
    }
}
