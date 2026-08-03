package com.fleetpulse.fleet_api.web.controller;

import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import com.fleetpulse.fleet_api.service.AlertThresholdService;
import com.fleetpulse.fleet_api.web.dto.request.CreateThresholdRequest;
import com.fleetpulse.fleet_api.web.dto.response.AlertThresholdResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alert-thresholds")
@RequiredArgsConstructor
public class AlertThresholdController {

    private final AlertThresholdService alertThresholdService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List alert thresholds")
    public List<AlertThresholdResponse> findAll(
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) String modelVersion) {
        return alertThresholdService.findAll(vehicleType, modelVersion);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_service')")
    @Operation(summary = "Internal: create alert threshold")
    public ResponseEntity<AlertThresholdResponse> create(@Valid @RequestBody CreateThresholdRequest request) {
        AlertThresholdResponse response = alertThresholdService.create(
                request.vehicleType(), request.modelVersion(), request.thresholdValue());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}