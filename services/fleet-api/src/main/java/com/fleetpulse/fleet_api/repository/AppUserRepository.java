package com.fleetpulse.fleet_api.repository;

import com.fleetpulse.fleet_api.domain.entity.AppUser;
import com.fleetpulse.fleet_api.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID>, JpaSpecificationExecutor<AppUser> {
    Optional<AppUser> findByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);

    List<AppUser> findByRole(UserRole role);

    long countByRole(UserRole role);
}