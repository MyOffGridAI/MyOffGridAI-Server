package com.myoffgridai.mcp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.service.ChatService;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.dto.KnowledgeSearchResultDto;
import com.myoffgridai.knowledge.service.KnowledgeService;
import com.myoffgridai.knowledge.service.SemanticSearchService;
import com.myoffgridai.mcp.config.McpAuthentication;
import com.myoffgridai.memory.dto.MemorySearchResultDto;
import com.myoffgridai.memory.service.MemoryService;
import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.model.SensorReading;
import com.myoffgridai.sensors.service.SensorService;
import com.myoffgridai.skills.dto.InventoryItemDto;
import com.myoffgridai.skills.model.InventoryCategory;
import com.myoffgridai.skills.model.InventoryItem;
import com.myoffgridai.skills.repository.InventoryItemRepository;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link McpToolsService}.
 */
@ExtendWith(MockitoExtension.class)
class McpToolsServiceTest {

    @Mock private SemanticSearchService semanticSearchService;
    @Mock private KnowledgeService knowledgeService;
    @Mock private MemoryService memoryService;
    @Mock private InventoryItemRepository inventoryItemRepository;
    @Mock private SensorService sensorService;
    @Mock private ChatService chatService;
    @Mock private SystemConfigService systemConfigService;

    private McpToolsService mcpToolsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID TOKEN_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mcpToolsService = new McpToolsService(
                semanticSearchService, knowledgeService, memoryService,
                inventoryItemRepository, sensorService, chatService,
                systemConfigService, objectMapper);

        SecurityContextHolder.getContext().setAuthentication(
                new McpAuthentication(OWNER_ID, TOKEN_ID));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Knowledge Tools ─────────────────────────────────────────────────────

    @Test
    void searchKnowledge_returnsJsonResults() {
        List<KnowledgeSearchResultDto> results = List.of(
                new KnowledgeSearchResultDto(UUID.randomUUID(), UUID.randomUUID(),
                        "test.pdf", "matching content", 1, 0, 0.95f)
        );
        when(semanticSearchService.search(OWNER_ID, "solar panels", 5)).thenReturn(results);

        String json = mcpToolsService.searchKnowledge("solar panels", 5);

        assertNotNull(json);
        assertTrue(json.contains("matching content"));
        assertTrue(json.contains("test.pdf"));
        verify(semanticSearchService).search(OWNER_ID, "solar panels", 5);
    }

    @Test
    void searchKnowledge_clampsTopK() {
        when(semanticSearchService.search(eq(OWNER_ID), anyString(), eq(20))).thenReturn(List.of());

        mcpToolsService.searchKnowledge("query", 50);

        verify(semanticSearchService).search(OWNER_ID, "query", 20);
    }

    @Test
    void listKnowledgeDocuments_returnsPaginatedJson() {
        var page = new PageImpl<KnowledgeDocumentDto>(List.of(), PageRequest.of(0, 20), 0);
        when(knowledgeService.listDocuments(eq(OWNER_ID), any(PageRequest.class))).thenReturn(page);

        String json = mcpToolsService.listKnowledgeDocuments(0, 20);

        assertNotNull(json);
        assertTrue(json.contains("totalElements"));
        assertTrue(json.contains("documents"));
    }

    // ── Memory Tools ────────────────────────────────────────────────────────

    @Test
    void searchMemories_returnsJsonResults() {
        when(memoryService.searchMemoriesWithScores(OWNER_ID, "preferences", 5))
                .thenReturn(List.of());

        String json = mcpToolsService.searchMemories("preferences", 5);

        assertNotNull(json);
        verify(memoryService).searchMemoriesWithScores(OWNER_ID, "preferences", 5);
    }

    // ── Inventory Tools ─────────────────────────────────────────────────────

    @Test
    void listInventory_returnsAllItems() {
        InventoryItem item = createTestInventoryItem();
        when(inventoryItemRepository.findByUserIdOrderByNameAsc(OWNER_ID)).thenReturn(List.of(item));

        String json = mcpToolsService.listInventory("");

        assertNotNull(json);
        assertTrue(json.contains("Rice"));
        verify(inventoryItemRepository).findByUserIdOrderByNameAsc(OWNER_ID);
    }

    @Test
    void listInventory_filtersByCategory() {
        InventoryItem item = createTestInventoryItem();
        when(inventoryItemRepository.findByUserIdAndCategory(OWNER_ID, InventoryCategory.FOOD))
                .thenReturn(List.of(item));

        String json = mcpToolsService.listInventory("FOOD");

        assertNotNull(json);
        assertTrue(json.contains("Rice"));
        verify(inventoryItemRepository).findByUserIdAndCategory(OWNER_ID, InventoryCategory.FOOD);
    }

    @Test
    void listInventory_handlesNullCategory() {
        when(inventoryItemRepository.findByUserIdOrderByNameAsc(OWNER_ID)).thenReturn(List.of());

        String json = mcpToolsService.listInventory(null);

        assertNotNull(json);
        verify(inventoryItemRepository).findByUserIdOrderByNameAsc(OWNER_ID);
    }

    @Test
    void addInventoryItem_createsAndReturnsItem() {
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> {
            InventoryItem item = invocation.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        String json = mcpToolsService.addInventoryItem("Beans", "FOOD", 10.0, "cans");

        assertNotNull(json);
        assertTrue(json.contains("Beans"));
        verify(inventoryItemRepository).save(any(InventoryItem.class));
    }

    @Test
    void updateInventoryItem_updatesQuantity() {
        UUID itemId = UUID.randomUUID();
        InventoryItem item = createTestInventoryItem();
        item.setId(itemId);
        when(inventoryItemRepository.findByIdAndUserId(itemId, OWNER_ID)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));

        String json = mcpToolsService.updateInventoryItem(itemId.toString(), 25.0);

        assertNotNull(json);
        assertTrue(json.contains("25.0"));
        verify(inventoryItemRepository).save(item);
    }

    @Test
    void updateInventoryItem_returnsErrorForNotFound() {
        UUID itemId = UUID.randomUUID();
        when(inventoryItemRepository.findByIdAndUserId(itemId, OWNER_ID)).thenReturn(Optional.empty());

        String json = mcpToolsService.updateInventoryItem(itemId.toString(), 25.0);

        assertTrue(json.contains("error"));
        assertTrue(json.contains("not found"));
    }

    @Test
    void deleteInventoryItem_deletesAndConfirms() {
        UUID itemId = UUID.randomUUID();
        InventoryItem item = createTestInventoryItem();
        item.setId(itemId);
        when(inventoryItemRepository.findByIdAndUserId(itemId, OWNER_ID)).thenReturn(Optional.of(item));

        String json = mcpToolsService.deleteInventoryItem(itemId.toString());

        assertTrue(json.contains("success"));
        verify(inventoryItemRepository).delete(item);
    }

    @Test
    void deleteInventoryItem_returnsErrorForNotFound() {
        UUID itemId = UUID.randomUUID();
        when(inventoryItemRepository.findByIdAndUserId(itemId, OWNER_ID)).thenReturn(Optional.empty());

        String json = mcpToolsService.deleteInventoryItem(itemId.toString());

        assertTrue(json.contains("error"));
    }

    @Test
    void getLowStockItems_filtersCorrectly() {
        InventoryItem lowItem = createTestInventoryItem();
        lowItem.setQuantity(2.0);
        lowItem.setLowStockThreshold(5.0);

        InventoryItem okItem = createTestInventoryItem();
        okItem.setQuantity(50.0);
        okItem.setLowStockThreshold(5.0);

        InventoryItem noThreshold = createTestInventoryItem();
        noThreshold.setLowStockThreshold(null);

        when(inventoryItemRepository.findByUserIdOrderByNameAsc(OWNER_ID))
                .thenReturn(List.of(lowItem, okItem, noThreshold));

        String json = mcpToolsService.getLowStockItems();

        assertNotNull(json);
        // Should contain only the low-stock item
        assertTrue(json.contains("2.0"));
    }

    // ── Sensor Tools ────────────────────────────────────────────────────────

    @Test
    void listSensors_returnsJson() {
        when(sensorService.listSensors(OWNER_ID)).thenReturn(List.of());

        String json = mcpToolsService.listSensors();

        assertNotNull(json);
        verify(sensorService).listSensors(OWNER_ID);
    }

    @Test
    void getLatestSensorReading_returnsReadingJson() {
        UUID sensorId = UUID.randomUUID();
        SensorReading reading = new SensorReading();
        when(sensorService.getLatestReading(sensorId, OWNER_ID)).thenReturn(Optional.of(reading));

        String json = mcpToolsService.getLatestSensorReading(sensorId.toString());

        assertNotNull(json);
        assertFalse(json.contains("No readings found"));
    }

    @Test
    void getLatestSensorReading_returnsMessageWhenNoReading() {
        UUID sensorId = UUID.randomUUID();
        when(sensorService.getLatestReading(sensorId, OWNER_ID)).thenReturn(Optional.empty());

        String json = mcpToolsService.getLatestSensorReading(sensorId.toString());

        assertTrue(json.contains("No readings found"));
    }

    // ── Conversation Tools ──────────────────────────────────────────────────

    @Test
    void listConversations_returnsPaginatedJson() {
        var page = new PageImpl<Conversation>(List.of(), PageRequest.of(0, 10), 0);
        when(chatService.getConversations(eq(OWNER_ID), eq(false), any(PageRequest.class))).thenReturn(page);

        String json = mcpToolsService.listConversations(0, 10);

        assertNotNull(json);
        assertTrue(json.contains("totalElements"));
        assertTrue(json.contains("conversations"));
    }

    @Test
    void listConversations_clampsPageSize() {
        var page = new PageImpl<Conversation>(List.of(), PageRequest.of(0, 50), 0);
        when(chatService.getConversations(eq(OWNER_ID), eq(false), any(PageRequest.class))).thenReturn(page);

        mcpToolsService.listConversations(0, 100);

        verify(chatService).getConversations(eq(OWNER_ID), eq(false), eq(PageRequest.of(0, 50)));
    }

    // ── System Tools ────────────────────────────────────────────────────────

    @Test
    void getSystemStatus_returnsJson() {
        SystemConfig config = new SystemConfig();
        when(systemConfigService.getConfig()).thenReturn(config);

        String json = mcpToolsService.getSystemStatus();

        assertNotNull(json);
        verify(systemConfigService).getConfig();
    }

    // ── Auth Context ────────────────────────────────────────────────────────

    @Test
    void tool_throwsWhenNoMcpAuthentication() {
        SecurityContextHolder.clearContext();

        assertThrows(IllegalStateException.class,
                () -> mcpToolsService.searchKnowledge("test", 5));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private InventoryItem createTestInventoryItem() {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setUserId(OWNER_ID);
        item.setName("Rice");
        item.setCategory(InventoryCategory.FOOD);
        item.setQuantity(10.0);
        item.setUnit("kg");
        return item;
    }
}
