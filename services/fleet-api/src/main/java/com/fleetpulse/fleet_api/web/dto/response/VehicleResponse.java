package com.fleetpulse.fleet_api.web.dto.response;

import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        UUID fleetId,
        String fleetName,
        String licensePlate,
        String brand,
        String model,
        VehicleType vehicleType,
        VehicleStatus status,
        LocalDateTime registeredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<VehicleImageResponse> photos

) {}