package com.fleetpulse.fleet_api.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InternalTelemetryRequest(
        @NotNull OffsetDateTime time,
        @NotNull UUID vehicleId,
        Double temperature,
        Double vibration,
        Double oilLevel,
        Double rpm,
        Double mileage
) {}