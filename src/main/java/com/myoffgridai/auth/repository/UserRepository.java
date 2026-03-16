package com.myoffgridai.auth.repository;

import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their unique username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by their email address.
     *
     * @param email the email to search for
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given username already exists.
     *
     * @param username the username to check
     * @return {@code true} if a user with that username exists
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether a user with the given email already exists.
     *
     * @param email the email to check
     * @return {@code true} if a user with that email exists
     */
    boolean existsByEmail(String email);

    /**
     * Retrieves all users with the specified role.
     *
     * @param role the role to filter by
     * @return a list of users with the given role
     */
    List<User> findAllByRole(Role role);

    /**
     * Counts the number of active users in the system.
     *
     * @return the count of users where {@code isActive} is {@code true}
     */
    long countByIsActiveTrue();

    /**
     * Retrieves all active users.
     *
     * @return a list of users where {@code isActive} is {@code true}
     */
    List<User> findByIsActiveTrue();
}
