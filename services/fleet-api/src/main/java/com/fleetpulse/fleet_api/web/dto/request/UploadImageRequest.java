package com.fleetpulse.fleet_api.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UploadImageRequest(

        @NotBlank(message = "L'URL de la photo est obligatoire")
        String url,

        @NotBlank(message = "Le publicId Cloudinary est obligatoire")
        String publicId

) {
}