package com.myoffgridai.mcp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.service.ChatService;
import com.myoffgridai.config.AppConstants;
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
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * MCP tool methods exposed to external AI clients via the Spring AI MCP server.
 *
 * <p>Each {@code @Tool}-annotated method is registered as an MCP-callable tool.
 * The authenticated user (OWNER) is resolved from the {@link McpAuthentication}
 * in the SecurityContext, set by {@link com.myoffgridai.mcp.config.McpAuthFilter}.</p>
 *
 * <p>All methods return JSON strings serialized via {@link ObjectMapper}.</p>
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Service
public class McpToolsService {

    private static final Logger log = LoggerFactory.getLogger(McpToolsService.class);

    private final SemanticSearchService semanticSearchService;
    private final KnowledgeService knowledgeService;
    private final MemoryService memoryService;
    private final InventoryItemRepository inventoryItemRepository;
    private final SensorService sensorService;
    private final ChatService chatService;
    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the MCP tools service with all required dependencies.
     *
     * @param semanticSearchService the knowledge semantic search service
     * @param knowledgeService      the knowledge document service
     * @param memoryService         the memory service
     * @param inventoryItemRepository the inventory item repository
     * @param sensorService         the sensor service
     * @param chatService           the chat service
     * @param systemConfigService   the system config service
     * @param objectMapper          the JSON object mapper
     */
    public McpToolsService(SemanticSearchService semanticSearchService,
                           KnowledgeService knowledgeService,
                           MemoryService memoryService,
                           InventoryItemRepository inventoryItemRepository,
                           SensorService sensorService,
                           ChatService chatService,
                           SystemConfigService systemConfigService,
                           ObjectMapper objectMapper) {
        this.semanticSearchService = semanticSearchService;
        this.knowledgeService = knowledgeService;
        this.memoryService = memoryService;
        this.inventoryItemRepository = inventoryItemRepository;
        this.sensorService = sensorService;
        this.chatService = chatService;
        this.systemConfigService = systemConfigService;
        this.objectMapper = objectMapper;
    }

    // ── Knowledge Tools ─────────────────────────────────────────────────────

    /**
     * Searches the knowledge base using semantic similarity.
     *
     * @param query the natural-language search query
     * @param topK  maximum number of results to return (1–20)
     * @return JSON array of matching knowledge chunks with similarity scores
     */
    @Tool(description = "Search the knowledge base using semantic similarity. Returns matching document chunks ranked by relevance.")
    public String searchKnowledge(
            @ToolParam(description = "Natural-language search query") String query,
            @ToolParam(description = "Maximum results to return (1-20, default 5)") int topK) {
        UUID userId = getAuthenticatedUserId();
        int clampedTopK = Math.max(1, Math.min(topK, 20));
        List<KnowledgeSearchResultDto> results = semanticSearchService.search(userId, query, clampedTopK);
        log.debug("MCP searchKnowledge: query='{}', topK={}, results={}", query, clampedTopK, results.size());
        return toJson(results);
    }

    /**
     * Lists documents in the knowledge base.
     *
     * @param page the page number (0-based)
     * @param size the page size (1–100)
     * @return JSON array of document summaries with pagination metadata
     */
    @Tool(description = "List documents in the knowledge base with pagination.")
    public String listKnowledgeDocuments(
            @ToolParam(description = "Page number (0-based, default 0)") int page,
            @ToolParam(description = "Page size (1-100, default 20)") int size) {
        UUID userId = getAuthenticatedUserId();
        int clampedSize = Math.max(1, Math.min(size, AppConstants.MAX_PAGE_SIZE));
        var docPage = knowledgeService.listDocuments(userId, PageRequest.of(Math.max(0, page), clampedSize));
        log.debug("MCP listKnowledgeDocuments: page={}, size={}, total={}", page, clampedSize, docPage.getTotalElements());
        return toJson(Map.of(
                "documents", docPage.getContent(),
                "totalElements", docPage.getTotalElements(),
                "page", docPage.getNumber(),
                "size", docPage.getSize()
        ));
    }

    // ── Memory Tools ────────────────────────────────────────────────────────

    /**
     * Searches the user's memories using semantic similarity.
     *
     * @param query the natural-language search query
     * @param topK  maximum number of results to return (1–20)
     * @return JSON array of matching memories with similarity scores
     */
    @Tool(description = "Search the user's memories using semantic similarity. Returns memories ranked by relevance.")
    public String searchMemories(
            @ToolParam(description = "Natural-language search query") String query,
            @ToolParam(description = "Maximum results to return (1-20, default 5)") int topK) {
        UUID userId = getAuthenticatedUserId();
        int clampedTopK = Math.max(1, Math.min(topK, 20));
        List<MemorySearchResultDto> results = memoryService.searchMemoriesWithScores(userId, query, clampedTopK);
        log.debug("MCP searchMemories: query='{}', topK={}, results={}", query, clampedTopK, results.size());
        return toJson(results);
    }

    // ── Inventory Tools ─────────────────────────────────────────────────────

    /**
     * Lists inventory items, optionally filtered by category.
     *
     * @param category optional category filter (FOOD, TOOLS, MEDICAL, SUPPLIES, SEEDS, EQUIPMENT, OTHER)
     * @return JSON array of inventory items
     */
    @Tool(description = "List inventory items. Optionally filter by category: FOOD, TOOLS, MEDICAL, SUPPLIES, SEEDS, EQUIPMENT, OTHER.")
    public String listInventory(
            @ToolParam(description = "Category filter (optional, leave empty for all). Values: FOOD, TOOLS, MEDICAL, SUPPLIES, SEEDS, EQUIPMENT, OTHER") String category) {
        UUID userId = getAuthenticatedUserId();
        List<InventoryItem> items;
        if (category != null && !category.isBlank()) {
            InventoryCategory cat = InventoryCategory.valueOf(category.toUpperCase().trim());
            items = inventoryItemRepository.findByUserIdAndCategory(userId, cat);
        } else {
            items = inventoryItemRepository.findByUserIdOrderByNameAsc(userId);
        }
        List<InventoryItemDto> dtos = items.stream().map(InventoryItemDto::from).toList();
        log.debug("MCP listInventory: category={}, count={}", category, dtos.size());
        return toJson(dtos);
    }

    /**
     * Adds a new inventory item.
     *
     * @param name     the item name
     * @param category the category (FOOD, TOOLS, MEDICAL, SUPPLIES, SEEDS, EQUIPMENT, OTHER)
     * @param quantity the quantity
     * @param unit     the unit of measurement (e.g., "kg", "liters", "pieces")
     * @return JSON of the created inventory item
     */
    @Tool(description = "Add a new inventory item to track supplies, food, tools, etc.")
    public String addInventoryItem(
            @ToolParam(description = "Item name") String name,
            @ToolParam(description = "Category: FOOD, TOOLS, MEDICAL, SUPPLIES, SEEDS, EQUIPMENT, OTHER") String category,
            @ToolParam(description = "Quantity") double quantity,
            @ToolParam(description = "Unit of measurement (e.g., kg, liters, pieces)") String unit) {
        UUID userId = getAuthenticatedUserId();
        InventoryItem item = new InventoryItem();
        item.setUserId(userId);
        item.setName(name);
        item.setCategory(InventoryCategory.valueOf(category.toUpperCase().trim()));
        item.setQuantity(quantity);
        item.setUnit(unit);
        item = inventoryItemRepository.save(item);
        log.info("MCP addInventoryItem: name='{}', category={}, userId={}", name, category, userId);
        return toJson(InventoryItemDto.from(item));
    }

    /**
     * Updates an existing inventory item's quantity.
     *
     * @param itemId   the inventory item ID
     * @param quantity the new quantity
     * @return JSON of the updated inventory item, or an error message
     */
    @Tool(description = "Update an inventory item's quantity.")
    public String updateInventoryItem(
            @ToolParam(description = "Inventory item ID (UUID)") String itemId,
            @ToolParam(description = "New quantity") double quantity) {
        UUID userId = getAuthenticatedUserId();
        Optional<InventoryItem> opt = inventoryItemRepository.findByIdAndUserId(UUID.fromString(itemId), userId);
        if (opt.isEmpty()) {
            return toJson(Map.of("error", "Inventory item not found: " + itemId));
        }
        InventoryItem item = opt.get();
        item.setQuantity(quantity);
        item = inventoryItemRepository.save(item);
        log.info("MCP updateInventoryItem: id={}, quantity={}", itemId, quantity);
        return toJson(InventoryItemDto.from(item));
    }

    /**
     * Deletes an inventory item.
     *
     * @param itemId the inventory item ID
     * @return JSON confirmation or error message
     */
    @Tool(description = "Delete an inventory item by ID.")
    public String deleteInventoryItem(
            @ToolParam(description = "Inventory item ID (UUID)") String itemId) {
        UUID userId = getAuthenticatedUserId();
        Optional<InventoryItem> opt = inventoryItemRepository.findByIdAndUserId(UUID.fromString(itemId), userId);
        if (opt.isEmpty()) {
            return toJson(Map.of("error", "Inventory item not found: " + itemId));
        }
        inventoryItemRepository.delete(opt.get());
        log.info("MCP deleteInventoryItem: id={}", itemId);
        return toJson(Map.of("success", true, "message", "Item deleted: " + itemId));
    }

    /**
     * Lists inventory items at or below low-stock threshold.
     *
     * @return JSON array of low-stock inventory items
     */
    @Tool(description = "List inventory items that are at or below their low-stock threshold.")
    public String getLowStockItems() {
        UUID userId = getAuthenticatedUserId();
        List<InventoryItem> allItems = inventoryItemRepository.findByUserIdOrderByNameAsc(userId);
        List<InventoryItemDto> lowStock = allItems.stream()
                .filter(item -> item.getLowStockThreshold() != null && item.getQuantity() <= item.getLowStockThreshold())
                .map(InventoryItemDto::from)
                .toList();
        log.debug("MCP getLowStockItems: count={}", lowStock.size());
        return toJson(lowStock);
    }

    // ── Sensor Tools ────────────────────────────────────────────────────────

    /**
     * Lists all configured sensors and their status.
     *
     * @return JSON array of sensors
     */
    @Tool(description = "List all configured sensors with their current status.")
    public String listSensors() {
        UUID userId = getAuthenticatedUserId();
        List<Sensor> sensors = sensorService.listSensors(userId);
        log.debug("MCP listSensors: count={}", sensors.size());
        return toJson(sensors);
    }

    /**
     * Gets the latest reading for a specific sensor.
     *
     * @param sensorId the sensor ID
     * @return JSON of the latest sensor reading, or a not-found message
     */
    @Tool(description = "Get the latest reading from a specific sensor.")
    public String getLatestSensorReading(
            @ToolParam(description = "Sensor ID (UUID)") String sensorId) {
        UUID userId = getAuthenticatedUserId();
        Optional<SensorReading> reading = sensorService.getLatestReading(UUID.fromString(sensorId), userId);
        if (reading.isEmpty()) {
            return toJson(Map.of("message", "No readings found for sensor: " + sensorId));
        }
        log.debug("MCP getLatestSensorReading: sensorId={}", sensorId);
        return toJson(reading.get());
    }

    // ── Conversation Tools ──────────────────────────────────────────────────

    /**
     * Lists recent conversations.
     *
     * @param page the page number (0-based)
     * @param size the page size (1–50)
     * @return JSON array of conversations with pagination metadata
     */
    @Tool(description = "List recent conversations with pagination.")
    public String listConversations(
            @ToolParam(description = "Page number (0-based, default 0)") int page,
            @ToolParam(description = "Page size (1-50, default 10)") int size) {
        UUID userId = getAuthenticatedUserId();
        int clampedSize = Math.max(1, Math.min(size, 50));
        var convPage = chatService.getConversations(userId, false, PageRequest.of(Math.max(0, page), clampedSize));
        log.debug("MCP listConversations: page={}, size={}, total={}", page, clampedSize, convPage.getTotalElements());
        return toJson(Map.of(
                "conversations", convPage.getContent(),
                "totalElements", convPage.getTotalElements(),
                "page", convPage.getNumber(),
                "size", convPage.getSize()
        ));
    }

    // ── System Tools ────────────────────────────────────────────────────────

    /**
     * Gets the current system status and configuration.
     *
     * @return JSON of the system configuration
     */
    @Tool(description = "Get the current system status and configuration including server version, device info, and feature flags.")
    public String getSystemStatus() {
        var config = systemConfigService.getConfig();
        log.debug("MCP getSystemStatus");
        return toJson(config);
    }

    // ── Internal Helpers ────────────────────────────────────────────────────

    /**
     * Extracts the owner user ID from the current MCP authentication context.
     *
     * @return the authenticated owner's user ID
     * @throws IllegalStateException if no MCP authentication is present
     */
    private UUID getAuthenticatedUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof McpAuthentication mcpAuth) {
            return mcpAuth.getOwnerUserId();
        }
        throw new IllegalStateException("No MCP authentication found in SecurityContext");
    }

    /**
     * Serializes an object to JSON string.
     *
     * @param value the object to serialize
     * @return JSON string
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize MCP tool response: {}", e.getMessage());
            return "{\"error\":\"Serialization failed\"}";
        }
    }
}
