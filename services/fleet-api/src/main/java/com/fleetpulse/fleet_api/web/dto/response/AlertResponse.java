package com.fleetpulse.fleet_api.web.dto.response;

import com.fleetpulse.fleet_api.domain.enums.AlertStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID vehicleId,
        String licensePlate,
        Double anomalyScoreValue,
        String modelVersion,
        AlertStatus status,
        LocalDateTime triggeredAt,
        LocalDateTime acknowledgedAt,
        LocalDateTime resolvedAt,
        UserSummaryResponse acknowledgedBy
) {}