package com.fleetpulse.fleet_api.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UploadImagesRequest(
        @NotEmpty
        List<@Valid UploadImageRequest> images
) {}