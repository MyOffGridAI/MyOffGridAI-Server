package com.myoffgridai.sensors.repository;

import com.myoffgridai.sensors.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Sensor} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Repository
public interface SensorRepository extends JpaRepository<Sensor, UUID> {

    /**
     * Finds all sensors for a user, ordered by name ascending.
     *
     * @param userId the user ID
     * @return list of sensors ordered by name
     */
    List<Sensor> findByUserIdOrderByNameAsc(UUID userId);

    /**
     * Finds a sensor by ID scoped to a specific user.
     *
     * @param id     the sensor ID
     * @param userId the user ID
     * @return the sensor, or empty if not found
     */
    Optional<Sensor> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Finds all active sensors for a user.
     *
     * @param userId the user ID
     * @return list of active sensors
     */
    List<Sensor> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Finds a sensor by its serial port path.
     *
     * @param portPath the serial port path
     * @return the sensor, or empty if not found
     */
    Optional<Sensor> findByPortPath(String portPath);

    /**
     * Finds all sensors that are currently active across all users.
     *
     * @return list of active sensors
     */
    List<Sensor> findByIsActiveTrue();

    /**
     * Counts the number of sensors owned by a user.
     *
     * @param userId the user ID
     * @return the sensor count
     */
    long countByUserId(UUID userId);

    /**
     * Deletes all sensors owned by a user.
     *
     * @param userId the user ID
     */
    void deleteByUserId(UUID userId);
}
