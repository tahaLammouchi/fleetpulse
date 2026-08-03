package com.fleetpulse.fleet_api.web.dto.response;

import com.fleetpulse.fleet_api.domain.enums.VehicleType;

import java.util.UUID;

public record AlertThresholdResponse(
        UUID id,
        VehicleType vehicleType,
        String modelVersion,
        Double thresholdValue
) {}