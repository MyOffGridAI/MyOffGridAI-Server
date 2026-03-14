package com.myoffgridai.skills.builtin;

import com.myoffgridai.config.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WeatherQuerySkillTest {

    private WeatherQuerySkill skill;
    private UUID userId;

    @BeforeEach
    void setUp() {
        skill = new WeatherQuerySkill();
        userId = UUID.randomUUID();
    }

    @Test
    void getSkillName_returnsWeatherQuery() {
        assertEquals(AppConstants.SKILL_WEATHER_QUERY, skill.getSkillName());
    }

    @Test
    void execute_returnsStubResponse() {
        Map<String, Object> result = skill.execute(userId, Map.of("location", "Portland, OR"));

        assertFalse((boolean) result.get("hasSensorData"));
        assertEquals(0, result.get("readingCount"));
        assertNotNull(result.get("conditions"));
        assertTrue(result.get("conditions").toString().contains("sensor"));
    }

    @Test
    void execute_withoutLocation_returnsStubResponse() {
        Map<String, Object> result = skill.execute(userId, Map.of());

        assertFalse((boolean) result.get("hasSensorData"));
        assertNotNull(result.get("conditions"));
    }
}
