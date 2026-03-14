package com.myoffgridai.memory.controller;

import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.memory.dto.*;
import com.myoffgridai.memory.model.Memory;
import com.myoffgridai.memory.model.MemoryImportance;
import com.myoffgridai.memory.service.MemoryService;
import com.myoffgridai.auth.model.User;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user memory management.
 *
 * <p>All endpoints require authentication. Users can only access their own memories.
 * Provides CRUD operations, semantic search, and bulk export.</p>
 */
@RestController
@RequestMapping(AppConstants.MEMORY_API_PATH)
public class MemoryController {

    private static final Logger log = LoggerFactory.getLogger(MemoryController.class);

    private final MemoryService memoryService;

    /**
     * Constructs the memory controller.
     *
     * @param memoryService the memory service
     */
    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * Lists memories for the authenticated user with optional filters.
     *
     * @param principal  the authenticated user
     * @param page       page number (default 0)
     * @param size       page size (default 20)
     * @param importance optional importance filter
     * @param tag        optional tag filter
     * @return paginated list of memories
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MemoryDto>>> listMemories(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) MemoryImportance importance,
            @RequestParam(required = false) String tag) {
        log.debug("Listing memories for user: {}", principal.getUsername());
        int clampedSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Page<Memory> memories = memoryService.getMemories(
                principal.getId(), importance, tag, PageRequest.of(page, clampedSize));

        List<MemoryDto> dtos = memories.getContent().stream()
                .map(memoryService::toDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.paginated(
                dtos, memories.getTotalElements(),
                memories.getNumber(), memories.getSize()));
    }

    /**
     * Gets a specific memory by ID.
     *
     * @param principal the authenticated user
     * @param id        the memory ID
     * @return the memory details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemoryDto>> getMemory(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID id) {
        log.debug("Getting memory: {} for user: {}", id, principal.getUsername());
        Memory memory = memoryService.getMemory(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(memoryService.toDto(memory)));
    }

    /**
     * Deletes a memory and its associated vector document.
     *
     * @param principal the authenticated user
     * @param id        the memory ID
     * @return empty success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMemory(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID id) {
        log.info("Deleting memory: {} for user: {}", id, principal.getUsername());
        memoryService.deleteMemory(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Memory deleted"));
    }

    /**
     * Updates the importance level of a memory.
     *
     * @param principal the authenticated user
     * @param id        the memory ID
     * @param request   the new importance level
     * @return the updated memory
     */
    @PutMapping("/{id}/importance")
    public ResponseEntity<ApiResponse<MemoryDto>> updateImportance(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID id,
            @Valid @RequestBody UpdateImportanceRequest request) {
        log.info("Updating importance for memory: {}", id);
        Memory updated = memoryService.updateImportance(id, principal.getId(), request.importance());
        return ResponseEntity.ok(ApiResponse.success(memoryService.toDto(updated)));
    }

    /**
     * Updates the tags of a memory.
     *
     * @param principal the authenticated user
     * @param id        the memory ID
     * @param request   the new tags
     * @return the updated memory
     */
    @PutMapping("/{id}/tags")
    public ResponseEntity<ApiResponse<MemoryDto>> updateTags(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID id,
            @RequestBody UpdateTagsRequest request) {
        log.info("Updating tags for memory: {}", id);
        Memory updated = memoryService.updateTags(id, principal.getId(), request.tags());
        return ResponseEntity.ok(ApiResponse.success(memoryService.toDto(updated)));
    }

    /**
     * Searches memories using vector similarity.
     *
     * @param principal the authenticated user
     * @param request   the search query and parameters
     * @return list of matching memories with similarity scores
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<MemorySearchResultDto>>> searchMemories(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody MemorySearchRequest request) {
        log.debug("Searching memories for user: {}", principal.getUsername());
        List<MemorySearchResultDto> results = memoryService.searchMemoriesWithScores(
                principal.getId(), request.query(), request.topK());
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Exports all memories for the authenticated user.
     *
     * @param principal the authenticated user
     * @return all memories as JSON (no pagination)
     */
    @GetMapping("/export")
    public ResponseEntity<ApiResponse<List<MemoryDto>>> exportMemories(
            @AuthenticationPrincipal User principal) {
        log.info("Exporting memories for user: {}", principal.getUsername());
        List<MemoryDto> dtos = memoryService.exportMemories(principal.getId())
                .stream()
                .map(memoryService::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
}
