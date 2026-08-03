package com.fleetpulse.fleet_api.web.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record FleetResponse(
        UUID id,
        String name,
        long vehicleCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}