package com.fleetpulse.fleet_api.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAlertRequest(
        @NotNull UUID vehicleId,
        @NotNull Double anomalyScoreValue,
        @NotNull String modelVersion,
        @NotNull LocalDateTime triggeredAt
) {}