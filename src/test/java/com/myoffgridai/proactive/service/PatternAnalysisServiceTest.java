package com.myoffgridai.proactive.service;

import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.repository.ConversationRepository;
import com.myoffgridai.memory.model.Memory;
import com.myoffgridai.memory.model.MemoryImportance;
import com.myoffgridai.memory.repository.MemoryRepository;
import com.myoffgridai.proactive.dto.PatternSummary;
import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.model.SensorType;
import com.myoffgridai.sensors.repository.SensorReadingRepository;
import com.myoffgridai.sensors.repository.SensorRepository;
import com.myoffgridai.skills.model.InventoryItem;
import com.myoffgridai.skills.model.PlannedTask;
import com.myoffgridai.skills.model.TaskStatus;
import com.myoffgridai.skills.repository.InventoryItemRepository;
import com.myoffgridai.skills.repository.PlannedTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatternAnalysisServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MemoryRepository memoryRepository;
    @Mock private SensorRepository sensorRepository;
    @Mock private SensorReadingRepository sensorReadingRepository;
    @Mock private InventoryItemRepository inventoryItemRepository;
    @Mock private PlannedTaskRepository plannedTaskRepository;

    private PatternAnalysisService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new PatternAnalysisService(
                conversationRepository, memoryRepository, sensorRepository,
                sensorReadingRepository, inventoryItemRepository, plannedTaskRepository);
        userId = UUID.randomUUID();
    }

    @Test
    void buildPatternSummary_withData_returnsSummary() {
        Conversation conv = new Conversation();
        conv.setTitle("Gardening plans");
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conv)));

        Memory memory = new Memory();
        memory.setContent("User is allergic to peanuts");
        when(memoryRepository.findByUserIdAndImportance(eq(userId), eq(MemoryImportance.HIGH), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(memory)));
        when(memoryRepository.findByUserIdAndImportance(eq(userId), eq(MemoryImportance.CRITICAL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Sensor sensor = new Sensor();
        sensor.setId(UUID.randomUUID());
        sensor.setType(SensorType.TEMPERATURE);
        when(sensorRepository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of(sensor));
        when(sensorReadingRepository.findAverageValueSince(eq(sensor.getId()), any(Instant.class)))
                .thenReturn(22.5);

        InventoryItem item = new InventoryItem();
        item.setName("Water Filter");
        item.setQuantity(1);
        item.setLowStockThreshold(5.0);
        when(inventoryItemRepository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of(item));

        PlannedTask task = new PlannedTask();
        task.setTitle("Install solar panels");
        when(plannedTaskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                eq(userId), eq(TaskStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        PatternSummary summary = service.buildPatternSummary(userId);

        assertTrue(summary.hasData());
        assertEquals(1, summary.recentConversationCount());
        assertEquals("Gardening plans", summary.recentConversationTitles().get(0));
        assertEquals("User is allergic to peanuts", summary.highImportanceMemories().get(0));
        assertEquals(22.5, summary.sensorAverages().get("TEMPERATURE"));
        assertEquals("Water Filter", summary.lowStockItems().get(0));
        assertEquals("Install solar panels", summary.activeTasks().get(0));
    }

    @Test
    void buildPatternSummary_noData_returnsEmptySummary() {
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(memoryRepository.findByUserIdAndImportance(eq(userId), any(MemoryImportance.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(sensorRepository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of());
        when(inventoryItemRepository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of());
        when(plannedTaskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                eq(userId), eq(TaskStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PatternSummary summary = service.buildPatternSummary(userId);

        assertFalse(summary.hasData());
        assertEquals(0, summary.recentConversationCount());
    }

    @Test
    void buildPatternSummary_conversationWithNullTitle_usesUntitled() {
        Conversation conv = new Conversation();
        conv.setTitle(null);
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conv)));
        when(memoryRepository.findByUserIdAndImportance(eq(userId), any(MemoryImportance.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(sensorRepository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of());
        when(inventoryItemRepository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of());
        when(plannedTaskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                eq(userId), eq(TaskStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PatternSummary summary = service.buildPatternSummary(userId);

        assertEquals("Untitled", summary.recentConversationTitles().get(0));
    }

    @Test
    void buildPatternSummary_sensorWithNullAverage_excluded() {
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(memoryRepository.findByUserIdAndImportance(eq(userId), any(MemoryImportance.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Sensor sensor = new Sensor();
        sensor.setId(UUID.randomUUID());
        sensor.setType(SensorType.HUMIDITY);
        when(sensorRepository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of(sensor));
        when(sensorReadingRepository.findAverageValueSince(eq(sensor.getId()), any(Instant.class)))
                .thenReturn(null);

        when(inventoryItemRepository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of());
        when(plannedTaskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                eq(userId), eq(TaskStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PatternSummary summary = service.buildPatternSummary(userId);

        assertTrue(summary.sensorAverages().isEmpty());
    }

    @Test
    void buildPatternSummary_itemAboveThreshold_notLowStock() {
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(memoryRepository.findByUserIdAndImportance(eq(userId), any(MemoryImportance.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(sensorRepository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of());

        InventoryItem item = new InventoryItem();
        item.setName("Rice");
        item.setQuantity(50);
        item.setLowStockThreshold(10.0);
        when(inventoryItemRepository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of(item));

        when(plannedTaskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                eq(userId), eq(TaskStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PatternSummary summary = service.buildPatternSummary(userId);

        assertTrue(summary.lowStockItems().isEmpty());
    }
}
