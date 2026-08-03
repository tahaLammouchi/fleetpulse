package com.fleetpulse.fleet_api.repository;

import com.fleetpulse.fleet_api.domain.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID>, JpaSpecificationExecutor<Alert> {
    long countByVehicleId(UUID vehicleId);
}