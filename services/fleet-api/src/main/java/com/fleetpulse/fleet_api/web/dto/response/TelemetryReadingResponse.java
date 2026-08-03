package com.fleetpulse.fleet_api.web.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TelemetryReadingResponse(
        OffsetDateTime time,
        UUID vehicleId,
        Double temperature,
        Double vibration,
        Double oilLevel,
        Double rpm,
        Double mileage
) {}