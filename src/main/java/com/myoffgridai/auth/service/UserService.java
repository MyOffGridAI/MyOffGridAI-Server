package com.myoffgridai.auth.service;

import com.myoffgridai.auth.dto.UserDetailDto;
import com.myoffgridai.auth.dto.UserSummaryDto;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.config.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service handling user CRUD operations, profile management,
 * and account lifecycle (activation/deactivation/deletion).
 *
 * <p>All modifications are transactional. Role-based access control
 * is enforced at the controller layer via {@code @PreAuthorize}.</p>
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    /**
     * Constructs the user service with the user repository.
     *
     * @param userRepository the user data access layer
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves a paginated list of all users as summary DTOs.
     *
     * @param page the page number (zero-based)
     * @param size the page size
     * @return a page of {@link UserSummaryDto}
     */
    public Page<UserSummaryDto> listUsers(int page, int size) {
        int clampedSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        log.debug("Listing users - page: {}, size: {}", page, clampedSize);
        return userRepository.findAll(PageRequest.of(page, clampedSize))
                .map(this::toSummary);
    }

    /**
     * Retrieves detailed user information by ID.
     *
     * @param id the user UUID
     * @return the {@link UserDetailDto}
     * @throws EntityNotFoundException if no user with the given ID exists
     */
    public UserDetailDto getUserById(UUID id) {
        log.debug("Fetching user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return toDetail(user);
    }

    /**
     * Updates a user's display name, email, and/or role.
     *
     * @param id          the user UUID
     * @param displayName the new display name (nullable — no change if null)
     * @param email       the new email (nullable — no change if null)
     * @param role        the new role (nullable — no change if null)
     * @return the updated {@link UserDetailDto}
     * @throws EntityNotFoundException if the user is not found
     */
    @Transactional
    public UserDetailDto updateUser(UUID id, String displayName, String email, Role role) {
        log.info("Updating user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

        if (displayName != null) {
            user.setDisplayName(displayName);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (role != null) {
            user.setRole(role);
        }

        user = userRepository.save(user);
        log.info("User updated: {}", user.getUsername());
        return toDetail(user);
    }

    /**
     * Deactivates a user account, preventing further logins.
     *
     * @param id the user UUID
     * @throws EntityNotFoundException if the user is not found
     */
    @Transactional
    public void deactivateUser(UUID id) {
        log.info("Deactivating user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User deactivated: {}", user.getUsername());
    }

    /**
     * Permanently deletes a user and all associated data.
     *
     * @param id the user UUID
     * @throws EntityNotFoundException if the user is not found
     */
    @Transactional
    public void deleteUser(UUID id) {
        log.info("Deleting user: {}", id);
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
        log.info("User deleted: {}", id);
    }

    private UserSummaryDto toSummary(User user) {
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getIsActive()
        );
    }

    private UserDetailDto toDetail(User user) {
        return new UserDetailDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt()
        );
    }
}
