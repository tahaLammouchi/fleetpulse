package com.fleetpulse.fleet_api.repository;

import com.fleetpulse.fleet_api.domain.entity.VehicleImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleImageRepository extends JpaRepository<VehicleImage, UUID> {

    List<VehicleImage> findByVehicleId(UUID vehicleId);

    long countByVehicleId(UUID vehicleId);

    boolean existsByPublicId(String publicId);
}