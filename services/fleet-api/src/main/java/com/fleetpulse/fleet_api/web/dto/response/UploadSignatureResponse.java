package com.fleetpulse.fleet_api.web.dto.response;

public record UploadSignatureResponse(
        String signature,
        long timestamp,
        String apiKey,
        String cloudName,
        String folder
) {}
