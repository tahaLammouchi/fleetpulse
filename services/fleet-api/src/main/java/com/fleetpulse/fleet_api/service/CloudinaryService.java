package com.fleetpulse.fleet_api.service;

import com.fleetpulse.fleet_api.web.dto.response.UploadSignatureResponse;

import java.util.UUID;

public interface CloudinaryService {

    UploadSignatureResponse generateSignature(UUID vehicleId);

    void deleteImage(String publicId);

}
