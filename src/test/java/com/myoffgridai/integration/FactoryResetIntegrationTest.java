package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.system.dto.FactoryResetRequest;
import com.myoffgridai.system.dto.InitializeRequest;
import com.myoffgridai.system.repository.SystemConfigRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the factory reset flow.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FactoryResetIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SystemConfigRepository systemConfigRepository;
    @Autowired private com.myoffgridai.auth.repository.UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
        systemConfigRepository.deleteAll();
    }

    @Test
    @Order(1)
    void factoryReset_fullFlow() throws Exception {
        // Step 1: Initialize system
        InitializeRequest initRequest = new InitializeRequest(
                "Test Instance", "resetowner", "Reset Owner",
                "reset@test.com", "pass");

        MvcResult initResult = mockMvc.perform(post("/api/system/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // Extract token
        String responseJson = initResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson)
                .path("data").path("accessToken").asText();

        // Step 2: Verify initialized
        mockMvc.perform(get("/api/system/status"))
                .andExpect(jsonPath("$.data.initialized").value(true));

        // Step 3: Factory reset with OWNER token
        FactoryResetRequest resetRequest = new FactoryResetRequest("RESET MY DEVICE");

        mockMvc.perform(post("/api/system/factory-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 4: Wait for async reset (give it a moment)
        Thread.sleep(3000);

        // Step 5: Verify system is now uninitialized
        mockMvc.perform(get("/api/system/status"))
                .andExpect(jsonPath("$.data.initialized").value(false));
    }

    @Test
    @Order(2)
    void factoryReset_wrongPhrase_returns400() throws Exception {
        // Initialize first
        InitializeRequest initRequest = new InitializeRequest(
                "Test Instance", "resetowner2", "Reset Owner",
                "reset2@test.com", "pass");

        MvcResult initResult = mockMvc.perform(post("/api/system/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(initResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Attempt reset with wrong phrase
        FactoryResetRequest resetRequest = new FactoryResetRequest("WRONG PHRASE");

        mockMvc.perform(post("/api/system/factory-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // System should still be initialized
        mockMvc.perform(get("/api/system/status"))
                .andExpect(jsonPath("$.data.initialized").value(true));
    }
}
