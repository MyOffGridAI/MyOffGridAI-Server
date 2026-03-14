package com.myoffgridai.auth.repository;

import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findAllByRole(Role role);

    long countByIsActiveTrue();

    List<User> findByIsActiveTrue();
}
