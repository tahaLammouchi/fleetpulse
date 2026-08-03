package com.fleetpulse.fleet_api.web.dto.response;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String fullName
) {}