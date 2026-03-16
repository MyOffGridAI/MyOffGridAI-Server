package com.myoffgridai.notification.repository;

import com.myoffgridai.notification.model.DeviceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DeviceRegistration} entities.
 */
@Repository
public interface DeviceRegistrationRepository extends JpaRepository<DeviceRegistration, UUID> {

    /**
     * Finds a device registration by user ID and device identifier.
     *
     * @param userId   the user ID
     * @param deviceId the device identifier
     * @return the matching registration, or empty
     */
    Optional<DeviceRegistration> findByUserIdAndDeviceId(UUID userId, String deviceId);

    /**
     * Finds all device registrations for a user.
     *
     * @param userId the user ID
     * @return list of registered devices
     */
    List<DeviceRegistration> findByUserId(UUID userId);

    /**
     * Deletes a device registration by user ID and device identifier.
     *
     * @param userId   the user ID
     * @param deviceId the device identifier
     */
    void deleteByUserIdAndDeviceId(UUID userId, String deviceId);
}
