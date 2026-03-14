package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.auth.dto.LoginRequest;
import com.myoffgridai.auth.dto.RegisterRequest;
import com.myoffgridai.knowledge.service.FileStorageService;
import com.myoffgridai.knowledge.service.OcrService;
import com.myoffgridai.memory.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InventoryIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private OllamaService ollamaService;
    @MockBean private EmbeddingService embeddingService;
    @MockBean private FileStorageService fileStorageService;
    @MockBean private OcrService ocrService;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        when(ollamaService.isAvailable()).thenReturn(false);
        accessToken = registerAndLogin("invuser_" + System.nanoTime(), "pass");
    }

    @Test
    void listInventory_empty_returns200() throws Exception {
        mockMvc.perform(get("/api/skills/inventory")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void createAndListInventoryItem_endToEnd() throws Exception {
        // Create item
        String createBody = """
                {"name":"Rice","category":"FOOD","quantity":10.0,"unit":"lb"}""";

        MvcResult createResult = mockMvc.perform(post("/api/skills/inventory")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Rice"))
                .andExpect(jsonPath("$.data.quantity").value(10.0))
                .andReturn();

        String itemId = objectMapper.readTree(
                createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // List items
        mockMvc.perform(get("/api/skills/inventory")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Rice"));

        // Update item
        mockMvc.perform(put("/api/skills/inventory/" + itemId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":20.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(20.0));

        // Delete item
        mockMvc.perform(delete("/api/skills/inventory/" + itemId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify empty
        mockMvc.perform(get("/api/skills/inventory")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void createInventoryItem_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/skills/inventory")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"category\":\"FOOD\",\"quantity\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateInventoryItem_notFound_returns404() throws Exception {
        mockMvc.perform(put("/api/skills/inventory/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":10}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteInventoryItem_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/skills/inventory/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void listInventory_withCategoryFilter_returnsFiltered() throws Exception {
        // Create FOOD item
        mockMvc.perform(post("/api/skills/inventory")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rice\",\"category\":\"FOOD\",\"quantity\":10}"))
                .andExpect(status().isOk());

        // Create TOOLS item
        mockMvc.perform(post("/api/skills/inventory")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hammer\",\"category\":\"TOOLS\",\"quantity\":1}"))
                .andExpect(status().isOk());

        // Filter by FOOD
        mockMvc.perform(get("/api/skills/inventory?category=FOOD")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Rice"));
    }

    @Test
    void inventoryEndpoints_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/skills/inventory"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/skills/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"category\":\"FOOD\",\"quantity\":1}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/skills/inventory/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":10}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/skills/inventory/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndLogin(String username, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                username, null, username, password, null);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}
