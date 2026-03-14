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

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, UUID> {

    Page<SensorReading> findBySensorIdOrderByRecordedAtDesc(UUID sensorId, Pageable pageable);

    List<SensorReading> findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(
            UUID sensorId, Instant after);

    Optional<SensorReading> findTopBySensorIdOrderByRecordedAtDesc(UUID sensorId);

    @Modifying
    void deleteBySensorId(UUID sensorId);

    @Modifying
    @Query("DELETE FROM SensorReading sr WHERE sr.sensor.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Query(value = """
            SELECT AVG(sr.value) FROM sensor_readings sr
            JOIN sensors s ON sr.sensor_id = s.id
            WHERE sr.sensor_id = :sensorId
            AND sr.recorded_at >= :since
            """, nativeQuery = true)
    Double findAverageValueSince(@Param("sensorId") UUID sensorId, @Param("since") Instant since);
}
