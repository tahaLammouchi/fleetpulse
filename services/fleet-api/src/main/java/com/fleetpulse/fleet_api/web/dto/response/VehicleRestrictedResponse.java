package com.fleetpulse.fleet_api.web.dto.response;

import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;

import java.util.UUID;

public record VehicleRestrictedResponse(
        UUID id,
        String licensePlate,
        String brand,
        String model,
        VehicleType vehicleType,
        VehicleStatus status
) {}