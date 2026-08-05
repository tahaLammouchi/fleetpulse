package com.fleetpulse.fleet_api.service;

import com.fleetpulse.fleet_api.domain.entity.Vehicle;
import com.fleetpulse.fleet_api.domain.entity.VehicleImage;
import com.fleetpulse.fleet_api.exception.BusinessRuleViolationException;
import com.fleetpulse.fleet_api.exception.ResourceNotFoundException;
import com.fleetpulse.fleet_api.repository.VehicleImageRepository;
import com.fleetpulse.fleet_api.repository.VehicleRepository;
import com.fleetpulse.fleet_api.web.dto.request.UploadImageRequest;
import com.fleetpulse.fleet_api.web.dto.response.UploadSignatureResponse;
import com.fleetpulse.fleet_api.web.dto.response.VehicleImageResponse;
import com.fleetpulse.fleet_api.web.mapper.VehicleImageMapper;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleImageService {

    private static final int MAX_PHOTOS = 5;

    private final VehicleRepository vehicleRepository;
    private final VehicleImageRepository vehicleImageRepository;
    private final VehicleImageMapper vehicleImageMapper;
    private final CloudinaryService cloudinaryService;

    public UploadSignatureResponse generateSignature(UUID vehicleId) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle", vehicleId));

        return cloudinaryService.generateSignature(vehicle.getId());
    }

    public List<VehicleImageResponse> uploadImages(
            UUID vehicleId,
            List<UploadImageRequest> imageRequests) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle", vehicleId));

        long count = vehicleImageRepository.countByVehicleId(vehicleId);

        if (count >= MAX_PHOTOS || count + imageRequests.size() > MAX_PHOTOS) {
            throw new BusinessRuleViolationException(
                    "Maximum 5 photos allowed per vehicle");
        }

        // Filter new images to avoid duplicates based on publicId
        List<VehicleImage> newImages = imageRequests.stream()
                .filter(request -> !vehicleImageRepository.existsByPublicId(request.publicId()))
                .map(request -> {
                    VehicleImage image = new VehicleImage();
                    image.setVehicle(vehicle);
                    image.setUrl(request.url());
                    image.setPublicId(request.publicId());
                    image.setUploadedAt(LocalDateTime.now());
                    return image;
                })
                .toList();

        return vehicleImageRepository.saveAll(newImages)
                .stream()
                .map(vehicleImageMapper::toResponse)
                .toList();

    }

    @Transactional(readOnly = true)
    public List<VehicleImageResponse> getVehicleImages(UUID vehicleId) {

        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException(
                    "Vehicle", vehicleId);
        }

        return vehicleImageRepository.findByVehicleId(vehicleId)
                .stream()
                .map(vehicleImageMapper::toResponse)
                .toList();
    }
    @Transactional
    public void deleteImages(UUID vehicleId, List<UUID> imageIds) {

        List<VehicleImage> images =
                vehicleImageRepository.findAllById(imageIds);

        if (images.size() != imageIds.size()) {
            throw new ResourceNotFoundException("One or more images not found");
        }

        for (VehicleImage image : images) {

            if (!image.getVehicle().getId().equals(vehicleId)) {
                throw new BusinessRuleViolationException(
                        "Image does not belong to this vehicle"
                );
            }

            cloudinaryService.deleteImage(image.getPublicId());
        }

        vehicleImageRepository.deleteAll(images);
    }
}
