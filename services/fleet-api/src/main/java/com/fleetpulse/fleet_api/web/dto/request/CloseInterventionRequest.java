package com.fleetpulse.fleet_api.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CloseInterventionRequest(
        @NotBlank String technicianReport
) {}