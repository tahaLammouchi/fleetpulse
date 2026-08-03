package com.fleetpulse.fleet_api.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InternalAnomalyScoreRequest(
        @NotNull OffsetDateTime time,
        @NotNull UUID vehicleId,
        @NotNull Double score,
        @NotNull String modelVersion
) {}