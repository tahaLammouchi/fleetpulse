package com.fleetpulse.fleet_api.web.dto.request;

import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateVehicleRequest(
        @NotNull UUID fleetId,
        @NotBlank String licensePlate,
        String brand,
        String model,
        @NotNull VehicleType vehicleType
) {}