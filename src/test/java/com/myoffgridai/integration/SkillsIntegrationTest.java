package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.auth.dto.LoginRequest;
import com.myoffgridai.auth.dto.RegisterRequest;
import com.myoffgridai.knowledge.service.FileStorageService;
import com.myoffgridai.knowledge.service.OcrService;
import com.myoffgridai.memory.service.EmbeddingService;
import com.myoffgridai.skills.model.Skill;
import com.myoffgridai.skills.model.SkillCategory;
import com.myoffgridai.skills.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SkillsIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SkillRepository skillRepository;

    @MockBean private OllamaService ollamaService;
    @MockBean private EmbeddingService embeddingService;
    @MockBean private FileStorageService fileStorageService;
    @MockBean private OcrService ocrService;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        when(ollamaService.isAvailable()).thenReturn(false);
        accessToken = registerAndLogin("skilluser_" + System.nanoTime(), "pass");
    }

    @Test
    void listSkills_afterSeeding_returnsBuiltInSkills() throws Exception {
        mockMvc.perform(get("/api/skills")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)));
    }

    @Test
    void getSkill_existingSkill_returnsSkill() throws Exception {
        Skill skill = skillRepository.findByName("weather-query")
                .orElseThrow();

        mockMvc.perform(get("/api/skills/" + skill.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("weather-query"));
    }

    @Test
    void getSkill_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/skills/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void executeSkill_weatherQuery_returnsResult() throws Exception {
        Skill skill = skillRepository.findByName("weather-query")
                .orElseThrow();

        String body = """
                {"skillId":"%s","params":{"location":"Portland"}}"""
                .formatted(skill.getId());

        mockMvc.perform(post("/api/skills/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void executeSkill_resourceCalculator_powerBudget_returnsResult() throws Exception {
        Skill skill = skillRepository.findByName("resource-calculator")
                .orElseThrow();

        String body = """
                {"skillId":"%s","params":{"calculationType":"power-budget","panelWatts":500,"batteryKwh":5,"dailyUsageWatts":2000,"sunHoursPerDay":5}}"""
                .formatted(skill.getId());

        mockMvc.perform(post("/api/skills/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void listExecutions_empty_returns200() throws Exception {
        mockMvc.perform(get("/api/skills/executions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void toggleSkill_asMember_returns403() throws Exception {
        Skill skill = skillRepository.findByName("weather-query")
                .orElseThrow();

        mockMvc.perform(patch("/api/skills/" + skill.getId() + "/toggle")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void skillEndpoints_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/skills/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/skills/executions"))
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
