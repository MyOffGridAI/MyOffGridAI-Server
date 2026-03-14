package com.myoffgridai.privacy.service;

import com.myoffgridai.ai.repository.ConversationRepository;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.knowledge.service.KnowledgeService;
import com.myoffgridai.memory.service.MemoryService;
import com.myoffgridai.privacy.dto.WipeResult;
import com.myoffgridai.proactive.service.InsightService;
import com.myoffgridai.proactive.service.NotificationService;
import com.myoffgridai.sensors.service.SensorService;
import com.myoffgridai.skills.repository.InventoryItemRepository;
import com.myoffgridai.skills.repository.PlannedTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Performs a complete data wipe for a user, cascade-deleting all user-owned
 * records in FK-respecting order. The wipe is atomic — either all steps
 * succeed or the transaction rolls back.
 */
@Service
public class DataWipeService {

    private static final Logger log = LoggerFactory.getLogger(DataWipeService.class);

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final MemoryService memoryService;
    private final KnowledgeService knowledgeService;
    private final SensorService sensorService;
    private final InsightService insightService;
    private final NotificationService notificationService;
    private final InventoryItemRepository inventoryItemRepository;
    private final PlannedTaskRepository plannedTaskRepository;
    private final AuditService auditService;

    /**
     * Constructs the data wipe service.
     *
     * @param messageRepository       the message repository
     * @param conversationRepository  the conversation repository
     * @param memoryService           the memory service
     * @param knowledgeService        the knowledge service
     * @param sensorService           the sensor service
     * @param insightService          the insight service
     * @param notificationService     the notification service
     * @param inventoryItemRepository the inventory item repository
     * @param plannedTaskRepository   the planned task repository
     * @param auditService            the audit service
     */
    public DataWipeService(MessageRepository messageRepository,
                           ConversationRepository conversationRepository,
                           MemoryService memoryService,
                           KnowledgeService knowledgeService,
                           SensorService sensorService,
                           InsightService insightService,
                           NotificationService notificationService,
                           InventoryItemRepository inventoryItemRepository,
                           PlannedTaskRepository plannedTaskRepository,
                           AuditService auditService) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.memoryService = memoryService;
        this.knowledgeService = knowledgeService;
        this.sensorService = sensorService;
        this.insightService = insightService;
        this.notificationService = notificationService;
        this.inventoryItemRepository = inventoryItemRepository;
        this.plannedTaskRepository = plannedTaskRepository;
        this.auditService = auditService;
    }

    /**
     * Wipes all data for a user in FK-respecting order.
     *
     * <p>Deletion order:
     * <ol>
     *   <li>Messages (references conversations)</li>
     *   <li>Conversations (references users)</li>
     *   <li>Memories + vector documents</li>
     *   <li>Knowledge documents, chunks, vectors, files</li>
     *   <li>Sensor readings + sensors</li>
     *   <li>Insights</li>
     *   <li>Notifications</li>
     *   <li>Inventory items</li>
     *   <li>Planned tasks</li>
     *   <li>Audit logs</li>
     * </ol>
     *
     * @param userId the user ID whose data to wipe
     * @return the wipe result
     */
    @Transactional
    public WipeResult wipeUser(UUID userId) {
        log.warn("Starting data wipe for user {}", userId);
        int steps = 0;

        try {
            // 1. Messages (FK → conversations)
            messageRepository.deleteByUserId(userId);
            steps++;
            log.info("Wipe step {}: messages deleted for user {}", steps, userId);

            // 2. Conversations (FK → users)
            conversationRepository.deleteByUserId(userId);
            steps++;
            log.info("Wipe step {}: conversations deleted for user {}", steps, userId);

            // 3. Memories + vector documents
            memoryService.deleteAllMemoriesForUser(userId);
            steps++;
            log.info("Wipe step {}: memories deleted for user {}", steps, userId);

            // 4. Knowledge documents, chunks, vectors, files
            knowledgeService.deleteAllForUser(userId);
            steps++;
            log.info("Wipe step {}: knowledge data deleted for user {}", steps, userId);

            // 5. Sensor readings + sensors
            sensorService.deleteAllForUser(userId);
            steps++;
            log.info("Wipe step {}: sensors deleted for user {}", steps, userId);

            // 6. Insights
            insightService.deleteAllForUser(userId);
            steps++;
            log.info("Wipe step {}: insights deleted for user {}", steps, userId);

            // 7. Notifications
            notificationService.deleteAllForUser(userId);
            steps++;
            log.info("Wipe step {}: notifications deleted for user {}", steps, userId);

            // 8. Inventory items
            inventoryItemRepository.deleteByUserId(userId);
            steps++;
            log.info("Wipe step {}: inventory items deleted for user {}", steps, userId);

            // 9. Planned tasks
            plannedTaskRepository.deleteByUserId(userId);
            steps++;
            log.info("Wipe step {}: planned tasks deleted for user {}", steps, userId);

            // 10. Audit logs for this user
            auditService.deleteByUserId(userId);
            steps++;
            log.info("Wipe step {}: audit logs deleted for user {}", steps, userId);

            log.warn("Data wipe completed for user {}: {} steps", userId, steps);
            return new WipeResult(userId, steps, Instant.now(), true);

        } catch (Exception e) {
            log.error("Data wipe failed at step {} for user {}: {}", steps + 1, userId, e.getMessage(), e);
            throw new RuntimeException("Data wipe failed at step " + (steps + 1) + ": " + e.getMessage(), e);
        }
    }
}
