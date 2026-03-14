package com.myoffgridai.skills.builtin;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.skills.service.BuiltInSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Built-in skill that queries local sensor data for weather/conditions.
 *
 * <p>In Phase 5, sensor data is not yet available (Phase 6 introduces sensors).
 * This implementation returns a graceful stub response indicating no sensors
 * are connected.</p>
 */
@Component
public class WeatherQuerySkill implements BuiltInSkill {

    private static final Logger log = LoggerFactory.getLogger(WeatherQuerySkill.class);

    @Override
    public String getSkillName() {
        return AppConstants.SKILL_WEATHER_QUERY;
    }

    /**
     * Queries current weather/sensor conditions for the user.
     *
     * @param userId the user's ID
     * @param params no parameters required
     * @return map with conditions, hasSensorData, readingCount, lastUpdated
     */
    @Override
    public Map<String, Object> execute(UUID userId, Map<String, Object> params) {
        log.info("Executing weather query for user: {}", userId);

        // Phase 5 stub — no SensorRepository yet
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conditions", "No local sensor data available. "
                + "Please connect sensors via the Sensor Dashboard.");
        result.put("hasSensorData", false);
        result.put("readingCount", 0);
        result.put("lastUpdated", null);

        return result;
    }
}
