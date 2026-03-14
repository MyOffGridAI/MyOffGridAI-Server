package com.myoffgridai.skills.builtin;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.sensors.model.DataFormat;
import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.model.SensorReading;
import com.myoffgridai.sensors.model.SensorType;
import com.myoffgridai.sensors.repository.SensorReadingRepository;
import com.myoffgridai.sensors.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherQuerySkillTest {

    @Mock private SensorRepository sensorRepository;
    @Mock private SensorReadingRepository readingRepository;

    private WeatherQuerySkill skill;
    private UUID userId;

    @BeforeEach
    void setUp() {
        skill = new WeatherQuerySkill(sensorRepository, readingRepository);
        userId = UUID.randomUUID();
    }

    @Test
    void getSkillName_returnsWeatherQuery() {
        assertEquals(AppConstants.SKILL_WEATHER_QUERY, skill.getSkillName());
    }

    @Test
    void execute_noActiveSensors_returnsStubResponse() {
        when(sensorRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(List.of());

        Map<String, Object> result = skill.execute(userId, Map.of());

        assertFalse((boolean) result.get("hasSensorData"));
        assertEquals(0, result.get("readingCount"));
        assertNotNull(result.get("conditions"));
        assertTrue(result.get("conditions").toString().contains("No local sensor data"));
    }

    @Test
    void execute_withActiveSensorsAndReadings_returnsConditions() {
        Sensor sensor = createSensor("Indoor Temp", "°C");
        when(sensorRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(List.of(sensor));

        SensorReading reading = new SensorReading();
        reading.setId(UUID.randomUUID());
        reading.setSensor(sensor);
        reading.setValue(23.45);
        reading.setRecordedAt(Instant.now());
        when(readingRepository.findTopBySensorIdOrderByRecordedAtDesc(sensor.getId()))
                .thenReturn(Optional.of(reading));

        Map<String, Object> result = skill.execute(userId, Map.of());

        assertTrue((boolean) result.get("hasSensorData"));
        assertEquals(1, result.get("readingCount"));
        String conditions = (String) result.get("conditions");
        assertTrue(conditions.contains("Indoor Temp"));
        assertTrue(conditions.contains("23.45"));
        assertTrue(conditions.contains("°C"));
        assertNotNull(result.get("lastUpdated"));
    }

    @Test
    void execute_withActiveSensorsButNoReadings_returnsNoReadingsMessage() {
        Sensor sensor = createSensor("Outdoor Temp", "°F");
        when(sensorRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(List.of(sensor));
        when(readingRepository.findTopBySensorIdOrderByRecordedAtDesc(sensor.getId()))
                .thenReturn(Optional.empty());

        Map<String, Object> result = skill.execute(userId, Map.of());

        assertFalse((boolean) result.get("hasSensorData"));
        assertEquals(0, result.get("readingCount"));
        assertTrue(result.get("conditions").toString().contains("no readings"));
    }

    @Test
    void execute_multipleSensors_aggregatesConditions() {
        Sensor sensor1 = createSensor("Temp", "°C");
        Sensor sensor2 = createSensor("Humidity", "%");
        when(sensorRepository.findByUserIdAndIsActiveTrue(userId))
                .thenReturn(List.of(sensor1, sensor2));

        SensorReading reading1 = createReading(sensor1, 22.0);
        SensorReading reading2 = createReading(sensor2, 55.0);
        when(readingRepository.findTopBySensorIdOrderByRecordedAtDesc(sensor1.getId()))
                .thenReturn(Optional.of(reading1));
        when(readingRepository.findTopBySensorIdOrderByRecordedAtDesc(sensor2.getId()))
                .thenReturn(Optional.of(reading2));

        Map<String, Object> result = skill.execute(userId, Map.of());

        assertTrue((boolean) result.get("hasSensorData"));
        assertEquals(2, result.get("readingCount"));
        String conditions = (String) result.get("conditions");
        assertTrue(conditions.contains("Temp"));
        assertTrue(conditions.contains("Humidity"));
        assertTrue(conditions.contains("; "));
    }

    private Sensor createSensor(String name, String unit) {
        Sensor sensor = new Sensor();
        sensor.setId(UUID.randomUUID());
        sensor.setUserId(userId);
        sensor.setName(name);
        sensor.setType(SensorType.TEMPERATURE);
        sensor.setPortPath("/dev/ttyUSB" + System.nanoTime());
        sensor.setBaudRate(9600);
        sensor.setDataFormat(DataFormat.CSV_LINE);
        sensor.setUnit(unit);
        sensor.setPollIntervalSeconds(30);
        sensor.setIsActive(true);
        return sensor;
    }

    private SensorReading createReading(Sensor sensor, double value) {
        SensorReading reading = new SensorReading();
        reading.setId(UUID.randomUUID());
        reading.setSensor(sensor);
        reading.setValue(value);
        reading.setRecordedAt(Instant.now());
        return reading;
    }
}
