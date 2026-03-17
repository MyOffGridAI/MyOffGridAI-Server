package com.myoffgridai.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.myoffgridai.ai.SourceTag;
import com.myoffgridai.ai.model.MessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MessageDto} serialization.
 *
 * <p>Validates that the new {@code sourceTag}, {@code judgeScore}, and
 * {@code judgeReason} fields serialize and deserialize correctly.</p>
 */
class MessageDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void serialization_includesAllFields() throws Exception {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        MessageDto dto = new MessageDto(
                id, MessageRole.ASSISTANT, "Hello world",
                42, false, "thinking content", 25.5, 1.5, "stop", 10,
                SourceTag.LOCAL, 8.5, "Good response", now
        );

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"sourceTag\":\"LOCAL\""));
        assertTrue(json.contains("\"judgeScore\":8.5"));
        assertTrue(json.contains("\"judgeReason\":\"Good response\""));
        assertTrue(json.contains("\"thinkingTokenCount\":10"));
        assertTrue(json.contains("\"content\":\"Hello world\""));
    }

    @Test
    void serialization_handlesNullJudgeFields() throws Exception {
        MessageDto dto = new MessageDto(
                UUID.randomUUID(), MessageRole.ASSISTANT, "content",
                10, false, null, null, null, null, null,
                null, null, null, Instant.now()
        );

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"sourceTag\":null"));
        assertTrue(json.contains("\"judgeScore\":null"));
        assertTrue(json.contains("\"judgeReason\":null"));
    }

    @Test
    void deserialization_roundTrips() throws Exception {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        MessageDto original = new MessageDto(
                id, MessageRole.ASSISTANT, "response",
                20, true, "think", 30.0, 2.0, "stop", 5,
                SourceTag.ENHANCED, 4.5, "Needs improvement", now
        );

        String json = objectMapper.writeValueAsString(original);
        MessageDto deserialized = objectMapper.readValue(json, MessageDto.class);

        assertEquals(original.id(), deserialized.id());
        assertEquals(original.role(), deserialized.role());
        assertEquals(original.content(), deserialized.content());
        assertEquals(original.sourceTag(), deserialized.sourceTag());
        assertEquals(original.judgeScore(), deserialized.judgeScore());
        assertEquals(original.judgeReason(), deserialized.judgeReason());
        assertEquals(original.thinkingTokenCount(), deserialized.thinkingTokenCount());
    }

    @Test
    void sourceTag_valuesSerializeCorrectly() throws Exception {
        MessageDto localDto = new MessageDto(
                UUID.randomUUID(), MessageRole.ASSISTANT, "c",
                1, false, null, null, null, null, null,
                SourceTag.LOCAL, null, null, Instant.now()
        );
        MessageDto enhancedDto = new MessageDto(
                UUID.randomUUID(), MessageRole.ASSISTANT, "c",
                1, false, null, null, null, null, null,
                SourceTag.ENHANCED, null, null, Instant.now()
        );

        String localJson = objectMapper.writeValueAsString(localDto);
        String enhancedJson = objectMapper.writeValueAsString(enhancedDto);

        assertTrue(localJson.contains("\"sourceTag\":\"LOCAL\""));
        assertTrue(enhancedJson.contains("\"sourceTag\":\"ENHANCED\""));
    }
}
