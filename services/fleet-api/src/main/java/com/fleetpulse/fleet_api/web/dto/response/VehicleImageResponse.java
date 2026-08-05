package com.fleetpulse.fleet_api.web.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleImageResponse(
        UUID id,
        String url,
        LocalDateTime uploadedAt
) {
}