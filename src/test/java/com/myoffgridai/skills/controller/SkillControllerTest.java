package com.myoffgridai.skills.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.skills.dto.CreateInventoryItemRequest;
import com.myoffgridai.skills.dto.SkillExecuteRequest;
import com.myoffgridai.skills.dto.UpdateInventoryItemRequest;
import com.myoffgridai.skills.model.*;
import com.myoffgridai.skills.repository.InventoryItemRepository;
import com.myoffgridai.skills.repository.SkillExecutionRepository;
import com.myoffgridai.skills.repository.SkillRepository;
import com.myoffgridai.skills.service.SkillExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SkillController.class)
@Import(TestSecurityConfig.class)
class SkillControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private SkillRepository skillRepository;
    @MockitoBean private SkillExecutionRepository executionRepository;
    @MockitoBean private InventoryItemRepository inventoryItemRepository;
    @MockitoBean private SkillExecutorService skillExecutorService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    @MockitoBean private AuthService authService;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User testUser;
    private User adminUser;
    private UUID userId;
    private Skill testSkill;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setRole(Role.ROLE_MEMBER);
        testUser.setPasswordHash("hash");

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("admin");
        adminUser.setDisplayName("Admin");
        adminUser.setRole(Role.ROLE_OWNER);
        adminUser.setPasswordHash("hash");

        testSkill = new Skill();
        testSkill.setId(UUID.randomUUID());
        testSkill.setName("test-skill");
        testSkill.setDisplayName("Test Skill");
        testSkill.setDescription("A test skill");
        testSkill.setVersion("1.0.0");
        testSkill.setAuthor("test");
        testSkill.setCategory(SkillCategory.HOMESTEAD);
        testSkill.setIsEnabled(true);
        testSkill.setIsBuiltIn(true);
        testSkill.setCreatedAt(Instant.now());
    }

    // ── Skill Management ─────────────────────────────────────────────────

    @Test
    void listSkills_returnsEnabledSkills() throws Exception {
        when(skillRepository.findByIsEnabledTrueOrderByDisplayNameAsc())
                .thenReturn(List.of(testSkill));

        mockMvc.perform(get("/api/skills").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("test-skill"));
    }

    @Test
    void listSkills_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSkill_returnsSkill() throws Exception {
        when(skillRepository.findById(testSkill.getId()))
                .thenReturn(Optional.of(testSkill));

        mockMvc.perform(get("/api/skills/" + testSkill.getId()).with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("test-skill"));
    }

    @Test
    void getSkill_notFound_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(skillRepository.findById(unknownId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/skills/" + unknownId).with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleSkill_asOwner_returns200() throws Exception {
        when(skillRepository.findById(testSkill.getId()))
                .thenReturn(Optional.of(testSkill));
        when(skillRepository.save(any(Skill.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(patch("/api/skills/" + testSkill.getId() + "/toggle")
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void toggleSkill_asMember_returns403() throws Exception {
        mockMvc.perform(patch("/api/skills/" + testSkill.getId() + "/toggle")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isForbidden());
    }

    // ── Skill Execution ──────────────────────────────────────────────────

    @Test
    void executeSkill_returnsExecutionResult() throws Exception {
        SkillExecution execution = new SkillExecution();
        execution.setId(UUID.randomUUID());
        execution.setSkill(testSkill);
        execution.setUserId(userId);
        execution.setStatus(ExecutionStatus.COMPLETED);
        execution.setOutputResult("{\"result\":\"ok\"}");
        execution.setStartedAt(Instant.now());
        execution.setCompletedAt(Instant.now());
        execution.setDurationMs(50L);

        when(skillExecutorService.execute(eq(testSkill.getId()), eq(userId), any()))
                .thenReturn(execution);

        SkillExecuteRequest request = new SkillExecuteRequest(testSkill.getId(), Map.of());
        mockMvc.perform(post("/api/skills/execute")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void listExecutions_returnsPaginated() throws Exception {
        SkillExecution execution = new SkillExecution();
        execution.setId(UUID.randomUUID());
        execution.setSkill(testSkill);
        execution.setUserId(userId);
        execution.setStatus(ExecutionStatus.COMPLETED);
        execution.setStartedAt(Instant.now());

        when(executionRepository.findByUserIdOrderByStartedAtDesc(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(execution)));

        mockMvc.perform(get("/api/skills/executions").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }

    // ── Inventory Convenience ────────────────────────────────────────────

    @Test
    void listInventory_returnsItems() throws Exception {
        InventoryItem item = createTestItem();
        when(inventoryItemRepository.findByUserIdOrderByNameAsc(userId))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/skills/inventory").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Rice"));
    }

    @Test
    void listInventory_withCategory_returnsFiltered() throws Exception {
        InventoryItem item = createTestItem();
        when(inventoryItemRepository.findByUserIdAndCategory(userId, InventoryCategory.FOOD))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/skills/inventory?category=FOOD").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("FOOD"));
    }

    @Test
    void createInventoryItem_returnsCreatedItem() throws Exception {
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(i -> {
            InventoryItem saved = i.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            return saved;
        });

        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                "Flour", InventoryCategory.FOOD, 5.0, "lb", null, null);

        mockMvc.perform(post("/api/skills/inventory")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Flour"));
    }

    @Test
    void createInventoryItem_missingName_returns400() throws Exception {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                "", InventoryCategory.FOOD, 5.0, "lb", null, null);

        mockMvc.perform(post("/api/skills/inventory")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateInventoryItem_returnsUpdated() throws Exception {
        UUID itemId = UUID.randomUUID();
        InventoryItem item = createTestItem();
        item.setId(itemId);
        when(inventoryItemRepository.findByIdAndUserId(itemId, userId))
                .thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateInventoryItemRequest request = new UpdateInventoryItemRequest(20.0, null, null);

        mockMvc.perform(put("/api/skills/inventory/" + itemId)
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateInventoryItem_notFound_returns404() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(inventoryItemRepository.findByIdAndUserId(itemId, userId))
                .thenReturn(Optional.empty());

        UpdateInventoryItemRequest request = new UpdateInventoryItemRequest(20.0, null, null);

        mockMvc.perform(put("/api/skills/inventory/" + itemId)
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteInventoryItem_returnsSuccess() throws Exception {
        UUID itemId = UUID.randomUUID();
        InventoryItem item = createTestItem();
        item.setId(itemId);
        when(inventoryItemRepository.findByIdAndUserId(itemId, userId))
                .thenReturn(Optional.of(item));

        mockMvc.perform(delete("/api/skills/inventory/" + itemId).with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(inventoryItemRepository).delete(item);
    }

    @Test
    void deleteInventoryItem_notFound_returns404() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(inventoryItemRepository.findByIdAndUserId(itemId, userId))
                .thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/skills/inventory/" + itemId).with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void inventoryEndpoints_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/skills/inventory"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/skills/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private InventoryItem createTestItem() {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setUserId(userId);
        item.setName("Rice");
        item.setCategory(InventoryCategory.FOOD);
        item.setQuantity(10.0);
        item.setUnit("lb");
        item.setCreatedAt(Instant.now());
        return item;
    }
}
