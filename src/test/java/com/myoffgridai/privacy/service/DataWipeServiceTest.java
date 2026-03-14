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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DataWipeService}.
 */
@ExtendWith(MockitoExtension.class)
class DataWipeServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MemoryService memoryService;

    @Mock
    private KnowledgeService knowledgeService;

    @Mock
    private SensorService sensorService;

    @Mock
    private InsightService insightService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private PlannedTaskRepository plannedTaskRepository;

    @Mock
    private AuditService auditService;

    private DataWipeService dataWipeService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        dataWipeService = new DataWipeService(
                messageRepository,
                conversationRepository,
                memoryService,
                knowledgeService,
                sensorService,
                insightService,
                notificationService,
                inventoryItemRepository,
                plannedTaskRepository,
                auditService
        );
        userId = UUID.randomUUID();
    }

    @Test
    void wipeUser_deletesAllDataInOrder() {
        WipeResult result = dataWipeService.wipeUser(userId);

        assertNotNull(result);

        var inOrder = inOrder(
                messageRepository,
                conversationRepository,
                memoryService,
                knowledgeService,
                sensorService,
                insightService,
                notificationService,
                inventoryItemRepository,
                plannedTaskRepository,
                auditService
        );

        inOrder.verify(messageRepository).deleteByUserId(userId);
        inOrder.verify(conversationRepository).deleteByUserId(userId);
        inOrder.verify(memoryService).deleteAllMemoriesForUser(userId);
        inOrder.verify(knowledgeService).deleteAllForUser(userId);
        inOrder.verify(sensorService).deleteAllForUser(userId);
        inOrder.verify(insightService).deleteAllForUser(userId);
        inOrder.verify(notificationService).deleteAllForUser(userId);
        inOrder.verify(inventoryItemRepository).deleteByUserId(userId);
        inOrder.verify(plannedTaskRepository).deleteByUserId(userId);
        inOrder.verify(auditService).deleteByUserId(userId);
    }

    @Test
    void wipeUser_returnsSuccessResult() {
        WipeResult result = dataWipeService.wipeUser(userId);

        assertTrue(result.success());
        assertEquals(10, result.stepsCompleted());
    }

    @Test
    void wipeUser_setsCorrectUserId() {
        WipeResult result = dataWipeService.wipeUser(userId);

        assertEquals(userId, result.targetUserId());
    }

    @Test
    void wipeUser_failureAtStep_throwsException() {
        doThrow(new RuntimeException("DB connection lost"))
                .when(knowledgeService).deleteAllForUser(userId);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> dataWipeService.wipeUser(userId));

        assertTrue(exception.getMessage().contains("Data wipe failed at step 4"));
        verify(messageRepository).deleteByUserId(userId);
        verify(conversationRepository).deleteByUserId(userId);
        verify(memoryService).deleteAllMemoriesForUser(userId);
        verify(knowledgeService).deleteAllForUser(userId);
        verifyNoInteractions(sensorService, insightService, notificationService);
        verifyNoMoreInteractions(inventoryItemRepository);
        verifyNoMoreInteractions(plannedTaskRepository);
    }
}
