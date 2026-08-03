package com.fleetpulse.fleet_api.web.dto.response;

import com.fleetpulse.fleet_api.domain.enums.InterventionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record InterventionResponse(
        UUID id,
        UUID vehicleId,
        String licensePlate,
        UserSummaryResponse technician,
        UUID alertId,
        InterventionStatus status,
        String description,
        String technicianReport,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}