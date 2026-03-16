package com.myoffgridai.sensors.repository;

import com.myoffgridai.sensors.model.SensorReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link SensorReading} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, UUID> {

    /**
     * Finds paginated sensor readings for a sensor, newest first.
     *
     * @param sensorId the sensor ID
     * @param pageable the pagination parameters
     * @return paginated sensor readings
     */
    Page<SensorReading> findBySensorIdOrderByRecordedAtDesc(UUID sensorId, Pageable pageable);

    /**
     * Finds all sensor readings recorded after a given timestamp, ordered ascending.
     *
     * @param sensorId the sensor ID
     * @param after    the cutoff timestamp
     * @return list of sensor readings after the cutoff
     */
    List<SensorReading> findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(
            UUID sensorId, Instant after);

    /**
     * Finds the most recent reading for a sensor.
     *
     * @param sensorId the sensor ID
     * @return the latest reading, or empty if none exist
     */
    Optional<SensorReading> findTopBySensorIdOrderByRecordedAtDesc(UUID sensorId);

    /**
     * Deletes all readings for a sensor.
     *
     * @param sensorId the sensor ID
     */
    @Modifying
    void deleteBySensorId(UUID sensorId);

    /**
     * Deletes all sensor readings owned by a user via the sensor relationship.
     *
     * @param userId the user ID
     */
    @Modifying
    @Query("DELETE FROM SensorReading sr WHERE sr.sensor.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    /**
     * Calculates the average sensor reading value since a given timestamp.
     *
     * @param sensorId the sensor ID
     * @param since    the cutoff timestamp
     * @return the average value, or null if no readings exist
     */
    @Query(value = """
            SELECT AVG(sr.value) FROM sensor_readings sr
            JOIN sensors s ON sr.sensor_id = s.id
            WHERE sr.sensor_id = :sensorId
            AND sr.recorded_at >= :since
            """, nativeQuery = true)
    Double findAverageValueSince(@Param("sensorId") UUID sensorId, @Param("since") Instant since);
}
