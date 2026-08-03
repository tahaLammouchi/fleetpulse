package com.fleetpulse.fleet_api.web.dto.request;

import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import jakarta.validation.constraints.NotNull;

public record CreateThresholdRequest(
        VehicleType vehicleType,
        @NotNull String modelVersion,
        @NotNull Double thresholdValue
) {}