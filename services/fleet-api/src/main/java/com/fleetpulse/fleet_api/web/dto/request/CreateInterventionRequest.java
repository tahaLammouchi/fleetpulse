package com.fleetpulse.fleet_api.web.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateInterventionRequest(
        UUID vehicleId,
        UUID technicianId,
        String description
) {}