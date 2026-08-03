package com.fleetpulse.fleet_api.web.dto.request;

import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import jakarta.validation.constraints.NotNull;

public record PatchVehicleStatusRequest(
        @NotNull VehicleStatus status
) {}