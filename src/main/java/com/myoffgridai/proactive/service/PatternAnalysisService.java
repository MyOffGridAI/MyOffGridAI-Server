package com.myoffgridai.proactive.service;

import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.repository.ConversationRepository;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.memory.model.Memory;
import com.myoffgridai.memory.model.MemoryImportance;
import com.myoffgridai.memory.repository.MemoryRepository;
import com.myoffgridai.proactive.dto.PatternSummary;
import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.repository.SensorReadingRepository;
import com.myoffgridai.sensors.repository.SensorRepository;
import com.myoffgridai.skills.model.InventoryItem;
import com.myoffgridai.skills.model.PlannedTask;
import com.myoffgridai.skills.model.TaskStatus;
import com.myoffgridai.skills.repository.InventoryItemRepository;
import com.myoffgridai.skills.repository.PlannedTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Analyzes a user's recent activity to identify patterns for insight generation.
 */
@Service
public class PatternAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(PatternAnalysisService.class);

    private final ConversationRepository conversationRepository;
    private final MemoryRepository memoryRepository;
    private final SensorRepository sensorRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PlannedTaskRepository plannedTaskRepository;

    /**
     * Constructs the pattern analysis service.
     *
     * @param conversationRepository the conversation repository
     * @param memoryRepository       the memory repository
     * @param sensorRepository       the sensor repository
     * @param sensorReadingRepository the sensor reading repository
     * @param inventoryItemRepository the inventory item repository
     * @param plannedTaskRepository   the planned task repository
     */
    public PatternAnalysisService(ConversationRepository conversationRepository,
                                  MemoryRepository memoryRepository,
                                  SensorRepository sensorRepository,
                                  SensorReadingRepository sensorReadingRepository,
                                  InventoryItemRepository inventoryItemRepository,
                                  PlannedTaskRepository plannedTaskRepository) {
        this.conversationRepository = conversationRepository;
        this.memoryRepository = memoryRepository;
        this.sensorRepository = sensorRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.plannedTaskRepository = plannedTaskRepository;
    }

    /**
     * Builds a pattern summary from the user's recent activity.
     *
     * @param userId the user ID
     * @return a summary of the user's recent patterns
     */
    public PatternSummary buildPatternSummary(UUID userId) {
        int windowDays = AppConstants.INSIGHT_ANALYSIS_WINDOW_DAYS;
        log.debug("Building pattern summary for user {} (window: {} days)", userId, windowDays);

        // Recent conversations
        List<Conversation> recentConversations = conversationRepository
                .findByUserIdOrderByUpdatedAtDesc(userId, PageRequest.of(0, 10))
                .getContent();
        List<String> titles = recentConversations.stream()
                .map(c -> c.getTitle() != null ? c.getTitle() : "Untitled")
                .toList();

        // High-importance memories
        List<String> highMemories = new ArrayList<>();
        memoryRepository.findByUserIdAndImportance(userId, MemoryImportance.HIGH, PageRequest.of(0, 10))
                .getContent().forEach(m -> highMemories.add(m.getContent()));
        memoryRepository.findByUserIdAndImportance(userId, MemoryImportance.CRITICAL, PageRequest.of(0, 5))
                .getContent().forEach(m -> highMemories.add(m.getContent()));

        // Sensor averages (last 24h)
        Map<String, Double> sensorAverages = new LinkedHashMap<>();
        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
        List<Sensor> sensors = sensorRepository.findByUserIdOrderByNameAsc(userId);
        for (Sensor sensor : sensors) {
            Double avg = sensorReadingRepository.findAverageValueSince(sensor.getId(), since24h);
            if (avg != null) {
                sensorAverages.put(sensor.getType().name(), avg);
            }
        }

        // Low-stock inventory items
        List<String> lowStockItems = new ArrayList<>();
        List<InventoryItem> allItems = inventoryItemRepository.findByUserIdOrderByNameAsc(userId);
        for (InventoryItem item : allItems) {
            if (item.getLowStockThreshold() != null && item.getQuantity() <= item.getLowStockThreshold()) {
                lowStockItems.add(item.getName());
            }
        }

        // Active planned tasks
        List<PlannedTask> tasks = plannedTaskRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, TaskStatus.ACTIVE, PageRequest.of(0, 10))
                .getContent();
        List<String> activeTasks = tasks.stream().map(PlannedTask::getTitle).toList();

        return new PatternSummary(
                recentConversations.size(),
                titles,
                highMemories,
                sensorAverages,
                lowStockItems,
                activeTasks,
                windowDays
        );
    }
}
