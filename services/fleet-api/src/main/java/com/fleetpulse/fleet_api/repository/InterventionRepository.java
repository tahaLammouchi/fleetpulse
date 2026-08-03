package com.fleetpulse.fleet_api.repository;

import com.fleetpulse.fleet_api.domain.entity.Intervention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterventionRepository extends JpaRepository<Intervention, UUID>, JpaSpecificationExecutor<Intervention> {
    long countByVehicleId(UUID vehicleId);
    List<Intervention> findAllByAlertId(UUID alertId);
    List<Intervention> findByVehicleId(UUID id);
}