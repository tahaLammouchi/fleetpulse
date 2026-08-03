package com.fleetpulse.fleet_api.repository;

import com.fleetpulse.fleet_api.domain.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID>, JpaSpecificationExecutor<Vehicle> {
    long countByFleetId(UUID fleetId);

    boolean existsByLicensePlate(String licensePlate);

    Page<Vehicle> findByFleetId(UUID fleetId, Pageable pageable);

    long count();
}