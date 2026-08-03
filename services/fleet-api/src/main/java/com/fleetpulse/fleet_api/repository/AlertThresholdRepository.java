package com.fleetpulse.fleet_api.repository;

import com.fleetpulse.fleet_api.domain.entity.AlertThreshold;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertThresholdRepository extends JpaRepository<AlertThreshold, UUID> {
    List<AlertThreshold> findByVehicleType(VehicleType vehicleType);
    List<AlertThreshold> findByModelVersion(String modelVersion);
    List<AlertThreshold> findByVehicleTypeAndModelVersion(VehicleType vehicleType, String modelVersion);
}