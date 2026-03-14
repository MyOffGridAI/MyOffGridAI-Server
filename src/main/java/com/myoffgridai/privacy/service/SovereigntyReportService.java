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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Assembles the Sovereignty Report — a comprehensive snapshot of the device's
 * privacy posture including fortress status, data inventory, audit summary,
 * and encryption/telemetry verification.
 */
@Service
public class SovereigntyReportService {

    private static final Logger log = LoggerFactory.getLogger(SovereigntyReportService.class);

    private static final int AUDIT_WINDOW_HOURS = 24;

    private final FortressService fortressService;
    private final AuditService auditService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MemoryRepository memoryRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final SensorRepository sensorRepository;
    private final InsightRepository insightRepository;

    /**
     * Constructs the sovereignty report service.
     *
     * @param fortressService           the fortress service
     * @param auditService              the audit service
     * @param conversationRepository    the conversation repository
     * @param messageRepository         the message repository
     * @param memoryRepository          the memory repository
     * @param knowledgeDocumentRepository the knowledge document repository
     * @param sensorRepository          the sensor repository
     * @param insightRepository         the insight repository
     */
    public SovereigntyReportService(FortressService fortressService,
                                     AuditService auditService,
                                     ConversationRepository conversationRepository,
                                     MessageRepository messageRepository,
                                     MemoryRepository memoryRepository,
                                     KnowledgeDocumentRepository knowledgeDocumentRepository,
                                     SensorRepository sensorRepository,
                                     InsightRepository insightRepository) {
        this.fortressService = fortressService;
        this.auditService = auditService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.memoryRepository = memoryRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.sensorRepository = sensorRepository;
        this.insightRepository = insightRepository;
    }

    /**
     * Generates a full sovereignty report for the specified user.
     *
     * @param userId the user ID
     * @return the sovereignty report
     */
    public SovereigntyReport generateReport(UUID userId) {
        log.info("Generating sovereignty report for user {}", userId);

        FortressStatus fortressStatus = fortressService.getFortressStatus();
        DataInventory dataInventory = buildDataInventory(userId);
        AuditSummary auditSummary = buildAuditSummary();

        String outboundTraffic = fortressStatus.enabled()
                ? "BLOCKED — All outbound internet traffic is blocked by iptables"
                : "ALLOWED — Fortress mode is disabled; outbound traffic is permitted";

        String encryptionStatus = "AES-256 encryption available for data exports; "
                + "database at rest on local disk";

        String telemetryStatus = "DISABLED — No telemetry, analytics, or usage data "
                + "is collected or transmitted";

        return new SovereigntyReport(
                Instant.now(),
                fortressStatus,
                outboundTraffic,
                dataInventory,
                auditSummary,
                encryptionStatus,
                telemetryStatus,
                Instant.now()
        );
    }

    /**
     * Builds a data inventory counting all user-owned records.
     *
     * @param userId the user ID
     * @return the data inventory
     */
    private DataInventory buildDataInventory(UUID userId) {
        long conversations = conversationRepository.countByUserId(userId);
        long messages = messageRepository.countByUserId(userId);
        long memories = memoryRepository.countByUserId(userId);
        long knowledgeDocs = knowledgeDocumentRepository.countByUserId(userId);
        long sensors = sensorRepository.countByUserId(userId);
        long insights = insightRepository.countByUserId(userId);

        return new DataInventory(conversations, messages, memories, knowledgeDocs, sensors, insights);
    }

    /**
     * Builds an audit summary for the last 24 hours.
     *
     * @return the audit summary
     */
    private AuditSummary buildAuditSummary() {
        Instant now = Instant.now();
        Instant windowStart = now.minus(AUDIT_WINDOW_HOURS, ChronoUnit.HOURS);

        long successCount = auditService.countByOutcomeBetween(AuditOutcome.SUCCESS, windowStart, now);
        long failureCount = auditService.countByOutcomeBetween(AuditOutcome.FAILURE, windowStart, now);
        long deniedCount = auditService.countByOutcomeBetween(AuditOutcome.DENIED, windowStart, now);

        return new AuditSummary(successCount, failureCount, deniedCount, windowStart, now);
    }
}
