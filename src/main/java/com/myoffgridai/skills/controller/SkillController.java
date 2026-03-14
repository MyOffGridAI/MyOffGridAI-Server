package com.myoffgridai.skills.controller;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.skills.dto.*;
import com.myoffgridai.skills.model.InventoryCategory;
import com.myoffgridai.skills.model.InventoryItem;
import com.myoffgridai.skills.model.Skill;
import com.myoffgridai.skills.model.SkillExecution;
import com.myoffgridai.skills.repository.InventoryItemRepository;
import com.myoffgridai.skills.repository.SkillExecutionRepository;
import com.myoffgridai.skills.repository.SkillRepository;
import com.myoffgridai.skills.service.SkillExecutorService;
import com.myoffgridai.common.exception.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for skill management, execution, execution history,
 * and inventory convenience endpoints.
 */
@RestController
@RequestMapping(AppConstants.SKILLS_API_PATH)
public class SkillController {

    private static final Logger log = LoggerFactory.getLogger(SkillController.class);

    private final SkillRepository skillRepository;
    private final SkillExecutionRepository executionRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SkillExecutorService skillExecutorService;

    /**
     * Constructs the skill controller.
     *
     * @param skillRepository         the skill repository
     * @param executionRepository     the skill execution repository
     * @param inventoryItemRepository the inventory item repository
     * @param skillExecutorService    the skill executor service
     */
    public SkillController(SkillRepository skillRepository,
                           SkillExecutionRepository executionRepository,
                           InventoryItemRepository inventoryItemRepository,
                           SkillExecutorService skillExecutorService) {
        this.skillRepository = skillRepository;
        this.executionRepository = executionRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.skillExecutorService = skillExecutorService;
    }

    // ── Skill Management ─────────────────────────────────────────────────

    /**
     * Lists all enabled skills.
     *
     * @return list of enabled skill DTOs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillDto>>> listSkills() {
        List<Skill> skills = skillRepository.findByIsEnabledTrueOrderByDisplayNameAsc();
        List<SkillDto> dtos = skills.stream().map(SkillDto::from).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * Gets a single skill by ID.
     *
     * @param skillId the skill ID
     * @return the skill DTO
     */
    @GetMapping("/{skillId}")
    public ResponseEntity<ApiResponse<SkillDto>> getSkill(@PathVariable UUID skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new EntityNotFoundException("Skill not found: " + skillId));
        return ResponseEntity.ok(ApiResponse.success(SkillDto.from(skill)));
    }

    /**
     * Toggles a skill's enabled/disabled state. Requires OWNER or ADMIN role.
     *
     * @param skillId the skill ID
     * @param body    must contain "enabled" boolean
     * @return the updated skill DTO
     */
    @PatchMapping("/{skillId}/toggle")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SkillDto>> toggleSkill(
            @PathVariable UUID skillId,
            @RequestBody Map<String, Boolean> body) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new EntityNotFoundException("Skill not found: " + skillId));

        boolean enabled = body.getOrDefault("enabled", !skill.getIsEnabled());
        skill.setIsEnabled(enabled);
        skill = skillRepository.save(skill);
        log.info("Skill '{}' toggled to enabled={}", skill.getName(), enabled);

        return ResponseEntity.ok(ApiResponse.success(SkillDto.from(skill),
                "Skill " + (enabled ? "enabled" : "disabled")));
    }

    // ── Skill Execution ──────────────────────────────────────────────────

    /**
     * Executes a skill by ID with the provided parameters.
     *
     * @param principal the authenticated user
     * @param request   the execution request with skillId and params
     * @return the skill execution result DTO
     */
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<SkillExecutionDto>> executeSkill(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody SkillExecuteRequest request) {
        log.info("User {} executing skill {}", principal.getId(), request.skillId());

        Map<String, Object> params = request.params() != null ? request.params() : Map.of();
        SkillExecution execution = skillExecutorService.execute(
                request.skillId(), principal.getId(), params);

        return ResponseEntity.ok(ApiResponse.success(SkillExecutionDto.from(execution)));
    }

    /**
     * Lists execution history for the authenticated user with pagination.
     *
     * @param principal the authenticated user
     * @param page      the page number (0-based)
     * @param size      the page size
     * @return paginated list of execution DTOs
     */
    @GetMapping("/executions")
    public ResponseEntity<ApiResponse<List<SkillExecutionDto>>> listExecutions(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Page<SkillExecution> executions = executionRepository.findByUserIdOrderByStartedAtDesc(
                principal.getId(), PageRequest.of(page, size));
        List<SkillExecutionDto> dtos = executions.getContent().stream()
                .map(SkillExecutionDto::from).toList();
        return ResponseEntity.ok(ApiResponse.paginated(
                dtos, executions.getTotalElements(), page, size));
    }

    // ── Inventory Convenience Endpoints ──────────────────────────────────

    /**
     * Lists inventory items for the authenticated user, optionally filtered by category.
     *
     * @param principal the authenticated user
     * @param category  optional category filter
     * @return list of inventory item DTOs
     */
    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<List<InventoryItemDto>>> listInventory(
            @AuthenticationPrincipal User principal,
            @RequestParam(required = false) InventoryCategory category) {
        List<InventoryItem> items;
        if (category != null) {
            items = inventoryItemRepository.findByUserIdAndCategory(principal.getId(), category);
        } else {
            items = inventoryItemRepository.findByUserIdOrderByNameAsc(principal.getId());
        }
        List<InventoryItemDto> dtos = items.stream().map(InventoryItemDto::from).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * Creates a new inventory item.
     *
     * @param principal the authenticated user
     * @param request   the create request
     * @return the created inventory item DTO
     */
    @PostMapping("/inventory")
    public ResponseEntity<ApiResponse<InventoryItemDto>> createInventoryItem(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody CreateInventoryItemRequest request) {
        InventoryItem item = new InventoryItem();
        item.setUserId(principal.getId());
        item.setName(request.name());
        item.setCategory(request.category());
        item.setQuantity(request.quantity());
        item.setUnit(request.unit());
        item.setNotes(request.notes());
        item.setLowStockThreshold(request.lowStockThreshold());
        item = inventoryItemRepository.save(item);
        log.info("Created inventory item '{}' for user {}", item.getName(), principal.getId());
        return ResponseEntity.ok(ApiResponse.success(InventoryItemDto.from(item), "Item added"));
    }

    /**
     * Updates an existing inventory item.
     *
     * @param principal the authenticated user
     * @param itemId    the item ID
     * @param request   the update request
     * @return the updated inventory item DTO
     */
    @PutMapping("/inventory/{itemId}")
    public ResponseEntity<ApiResponse<InventoryItemDto>> updateInventoryItem(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateInventoryItemRequest request) {
        InventoryItem item = inventoryItemRepository.findByIdAndUserId(itemId, principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + itemId));

        if (request.quantity() != null) {
            item.setQuantity(request.quantity());
        }
        if (request.notes() != null) {
            item.setNotes(request.notes());
        }
        if (request.lowStockThreshold() != null) {
            item.setLowStockThreshold(request.lowStockThreshold());
        }

        item = inventoryItemRepository.save(item);
        log.info("Updated inventory item '{}' for user {}", item.getName(), principal.getId());
        return ResponseEntity.ok(ApiResponse.success(InventoryItemDto.from(item), "Item updated"));
    }

    /**
     * Deletes an inventory item.
     *
     * @param principal the authenticated user
     * @param itemId    the item ID
     * @return success response
     */
    @DeleteMapping("/inventory/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteInventoryItem(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID itemId) {
        InventoryItem item = inventoryItemRepository.findByIdAndUserId(itemId, principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + itemId));
        inventoryItemRepository.delete(item);
        log.info("Deleted inventory item '{}' for user {}", item.getName(), principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Item deleted"));
    }
}
