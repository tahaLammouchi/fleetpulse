package com.fleetpulse.fleet_api.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fleetpulse.fleet_api.exception.CloudinaryException;
import com.fleetpulse.fleet_api.web.dto.response.UploadSignatureResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Override
    public UploadSignatureResponse generateSignature(UUID vehicleId) {

        long timestamp = Instant.now().getEpochSecond();

        String folder = "fleetpulse/vehicles/" + vehicleId;

        Map<String, Object> params = new HashMap<>();
        params.put("folder", folder);
        params.put("timestamp", timestamp);

        String signature = cloudinary.apiSignRequest(params, apiSecret);

        return new UploadSignatureResponse(
                signature,
                timestamp,
                apiKey,
                cloudName,
                folder
        );
    }
    @Override
    public void deleteImage(String publicId) throws CloudinaryException {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new CloudinaryException("Unable to delete image from Cloudinary : " + e.getMessage());
        }
    }
}