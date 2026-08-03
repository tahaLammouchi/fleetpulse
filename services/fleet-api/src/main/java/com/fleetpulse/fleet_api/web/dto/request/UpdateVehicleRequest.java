package com.fleetpulse.fleet_api.web.dto.request;

import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;

import java.util.UUID;

public record UpdateVehicleRequest(

        String licensePlate,

        String brand,

        String model,

        VehicleType vehicleType,

        VehicleStatus status,

        UUID fleetId
) {}