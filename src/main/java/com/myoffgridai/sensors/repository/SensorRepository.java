package com.myoffgridai.sensors.repository;

import com.myoffgridai.sensors.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, UUID> {

    List<Sensor> findByUserIdOrderByNameAsc(UUID userId);

    Optional<Sensor> findByIdAndUserId(UUID id, UUID userId);

    List<Sensor> findByUserIdAndIsActiveTrue(UUID userId);

    Optional<Sensor> findByPortPath(String portPath);

    List<Sensor> findByIsActiveTrue();

    void deleteByUserId(UUID userId);
}
