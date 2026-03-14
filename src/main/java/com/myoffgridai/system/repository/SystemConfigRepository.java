package com.myoffgridai.system.repository;

import com.myoffgridai.system.model.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID> {

    @Query("SELECT s FROM SystemConfig s")
    Optional<SystemConfig> findFirst();
}
