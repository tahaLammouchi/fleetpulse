package com.fleetpulse.fleet_api.web.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record DeleteImagesRequest(
        @NotEmpty
        List<UUID> imageIds
) {}
