package com.fleetpulse.fleet_api.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignInterventionRequest(
        @NotNull UUID technicianId
) {}