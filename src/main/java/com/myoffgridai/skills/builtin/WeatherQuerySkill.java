package com.myoffgridai.skills.builtin;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.model.SensorReading;
import com.myoffgridai.sensors.repository.SensorReadingRepository;
import com.myoffgridai.sensors.repository.SensorRepository;
import com.myoffgridai.skills.service.BuiltInSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Built-in skill that queries local sensor data for weather/conditions.
 *
 * <p>Fetches all active sensors for the user, retrieves the latest reading
 * from each, and builds a conditions summary string.</p>
 */
@Component
public class WeatherQuerySkill implements BuiltInSkill {

    private static final Logger log = LoggerFactory.getLogger(WeatherQuerySkill.class);

    private final SensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;

    /**
     * Constructs the weather query skill with sensor data access.
     *
     * @param sensorRepository  the sensor repository
     * @param readingRepository the sensor reading repository
     */
    public WeatherQuerySkill(SensorRepository sensorRepository,
                             SensorReadingRepository readingRepository) {
        this.sensorRepository = sensorRepository;
        this.readingRepository = readingRepository;
    }

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

        List<Sensor> activeSensors = sensorRepository.findByUserIdAndIsActiveTrue(userId);

        if (activeSensors.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("conditions", "No local sensor data available. "
                    + "Please connect sensors via the Sensor Dashboard.");
            result.put("hasSensorData", false);
            result.put("readingCount", 0);
            result.put("lastUpdated", null);
            return result;
        }

        StringBuilder conditions = new StringBuilder();
        int readingCount = 0;
        Instant latestTimestamp = null;

        for (Sensor sensor : activeSensors) {
            Optional<SensorReading> latest =
                    readingRepository.findTopBySensorIdOrderByRecordedAtDesc(sensor.getId());
            if (latest.isPresent()) {
                SensorReading reading = latest.get();
                readingCount++;
                String unit = sensor.getUnit() != null ? sensor.getUnit() : "";
                if (!conditions.isEmpty()) {
                    conditions.append("; ");
                }
                conditions.append(String.format("%s: %.2f%s",
                        sensor.getName(), reading.getValue(), unit));

                if (latestTimestamp == null || reading.getRecordedAt().isAfter(latestTimestamp)) {
                    latestTimestamp = reading.getRecordedAt();
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        if (readingCount > 0) {
            result.put("conditions", conditions.toString());
            result.put("hasSensorData", true);
        } else {
            result.put("conditions", "Sensors are connected but no readings available yet.");
            result.put("hasSensorData", false);
        }
        result.put("readingCount", readingCount);
        result.put("lastUpdated", latestTimestamp);

        return result;
    }
}
