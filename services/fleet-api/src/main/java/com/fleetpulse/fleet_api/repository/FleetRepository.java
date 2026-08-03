package com.fleetpulse.fleet_api.repository;

import com.fleetpulse.fleet_api.domain.entity.Fleet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface FleetRepository extends JpaRepository<Fleet, UUID>, JpaSpecificationExecutor<Fleet> {
    long count();
}