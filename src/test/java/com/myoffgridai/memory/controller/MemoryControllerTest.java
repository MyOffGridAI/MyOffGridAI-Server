package com.myoffgridai.memory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.memory.dto.MemoryDto;
import com.myoffgridai.memory.dto.MemorySearchResultDto;
import com.myoffgridai.memory.dto.UpdateImportanceRequest;
import com.myoffgridai.memory.dto.UpdateTagsRequest;
import com.myoffgridai.memory.model.Memory;
import com.myoffgridai.memory.model.MemoryImportance;
import com.myoffgridai.memory.service.MemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemoryController.class)
@Import(TestSecurityConfig.class)
class MemoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private MemoryService memoryService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    @MockitoBean private AuthService authService;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setRole(Role.ROLE_MEMBER);
        testUser.setPasswordHash("hash");
    }

    @Test
    void listMemories_returnsPagedResults() throws Exception {
        Memory memory = createTestMemory();
        when(memoryService.getMemories(eq(userId), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(memory)));
        when(memoryService.toDto(any(Memory.class))).thenReturn(createTestDto(memory));

        mockMvc.perform(get("/api/memory")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getMemory_returnsMemory() throws Exception {
        UUID memoryId = UUID.randomUUID();
        Memory memory = createTestMemory();
        memory.setId(memoryId);
        when(memoryService.getMemory(memoryId, userId)).thenReturn(memory);
        when(memoryService.toDto(memory)).thenReturn(createTestDto(memory));

        mockMvc.perform(get("/api/memory/" + memoryId)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("test fact"));
    }

    @Test
    void getMemory_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/memory/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteMemory_returnsSuccess() throws Exception {
        UUID memoryId = UUID.randomUUID();

        mockMvc.perform(delete("/api/memory/" + memoryId)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(memoryService).deleteMemory(memoryId, userId);
    }

    @Test
    void updateImportance_returnsUpdated() throws Exception {
        UUID memoryId = UUID.randomUUID();
        Memory memory = createTestMemory();
        memory.setId(memoryId);
        memory.setImportance(MemoryImportance.CRITICAL);
        when(memoryService.updateImportance(memoryId, userId, MemoryImportance.CRITICAL))
                .thenReturn(memory);
        when(memoryService.toDto(memory)).thenReturn(createTestDto(memory));

        mockMvc.perform(put("/api/memory/" + memoryId + "/importance")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateImportanceRequest(MemoryImportance.CRITICAL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateTags_returnsUpdated() throws Exception {
        UUID memoryId = UUID.randomUUID();
        Memory memory = createTestMemory();
        memory.setId(memoryId);
        memory.setTags("new-tag");
        when(memoryService.updateTags(memoryId, userId, "new-tag")).thenReturn(memory);
        when(memoryService.toDto(memory)).thenReturn(createTestDto(memory));

        mockMvc.perform(put("/api/memory/" + memoryId + "/tags")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTagsRequest("new-tag"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void searchMemories_returnsResults() throws Exception {
        MemoryDto dto = new MemoryDto(UUID.randomUUID(), userId, "fact", MemoryImportance.HIGH,
                "tag", null, Instant.now(), Instant.now(), null, 0, false);
        when(memoryService.searchMemoriesWithScores(eq(userId), anyString(), anyInt()))
                .thenReturn(List.of(new MemorySearchResultDto(dto, 0.95f)));

        mockMvc.perform(post("/api/memory/search")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test query\",\"topK\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].similarityScore").value(0.95));
    }

    @Test
    void exportMemories_returnsAllMemories() throws Exception {
        Memory memory = createTestMemory();
        when(memoryService.exportMemories(userId)).thenReturn(List.of(memory));
        when(memoryService.toDto(memory)).thenReturn(createTestDto(memory));

        mockMvc.perform(get("/api/memory/export")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].content").value("test fact"));
    }

    private Memory createTestMemory() {
        Memory memory = new Memory();
        memory.setId(UUID.randomUUID());
        memory.setUserId(userId);
        memory.setContent("test fact");
        memory.setImportance(MemoryImportance.MEDIUM);
        memory.setCreatedAt(Instant.now());
        memory.setUpdatedAt(Instant.now());
        return memory;
    }

    private MemoryDto createTestDto(Memory memory) {
        return new MemoryDto(memory.getId(), memory.getUserId(), memory.getContent(), memory.getImportance(),
                memory.getTags(), memory.getSourceConversationId(),
                memory.getCreatedAt(), memory.getUpdatedAt(),
                memory.getLastAccessedAt(), memory.getAccessCount(), memory.isShared());
    }
}
