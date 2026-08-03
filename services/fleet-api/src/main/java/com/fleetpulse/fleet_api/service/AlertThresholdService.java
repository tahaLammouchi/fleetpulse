package com.fleetpulse.fleet_api.service;

import com.fleetpulse.fleet_api.domain.entity.AlertThreshold;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import com.fleetpulse.fleet_api.exception.BusinessRuleViolationException;
import com.fleetpulse.fleet_api.repository.AlertThresholdRepository;
import com.fleetpulse.fleet_api.web.dto.response.AlertThresholdResponse;
import com.fleetpulse.fleet_api.web.mapper.AlertThresholdMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertThresholdService {

    private final AlertThresholdRepository repository;
    private final AlertThresholdMapper mapper;

    public List<AlertThresholdResponse> findAll(VehicleType vehicleType, String modelVersion) {
        List<AlertThreshold> thresholds;
        if (vehicleType != null && modelVersion != null) {
            thresholds = repository.findByVehicleTypeAndModelVersion(vehicleType, modelVersion);
        } else if (vehicleType != null) {
            thresholds = repository.findByVehicleType(vehicleType);
        } else if (modelVersion != null) {
            thresholds = repository.findByModelVersion(modelVersion);
        } else {
            thresholds = repository.findAll();
        }
        return thresholds.stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public AlertThresholdResponse create(VehicleType vehicleType, String modelVersion, Double thresholdValue) {
        if (!repository.findByVehicleTypeAndModelVersion(
                vehicleType, modelVersion).isEmpty()) {
            throw new BusinessRuleViolationException("Threshold for vehicle type " + vehicleType +
                    " and model version " + modelVersion + " already exists.");
        }
        AlertThreshold threshold = new AlertThreshold();
        threshold.setVehicleType(vehicleType);
        threshold.setModelVersion(modelVersion);
        threshold.setThresholdValue(thresholdValue);
        threshold = repository.save(threshold);
        return mapper.toResponse(threshold);
    }
}