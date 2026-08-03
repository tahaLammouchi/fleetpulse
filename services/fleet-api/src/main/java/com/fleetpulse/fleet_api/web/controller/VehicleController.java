package com.fleetpulse.fleet_api.web.controller;

import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import com.fleetpulse.fleet_api.service.VehicleService;
import com.fleetpulse.fleet_api.web.dto.request.CreateVehicleRequest;
import com.fleetpulse.fleet_api.web.dto.request.PatchVehicleStatusRequest;
import com.fleetpulse.fleet_api.web.dto.request.UpdateVehicleRequest;
import com.fleetpulse.fleet_api.web.dto.response.TelemetryReadingResponse;
import com.fleetpulse.fleet_api.web.dto.response.VehicleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a vehicle")
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody CreateVehicleRequest request) {
        VehicleResponse response = vehicleService.create(
                request.fleetId(), request.licensePlate(), request.brand(), request.model(), request.vehicleType());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER')")
    @Operation(summary = "List vehicles with filters")
    public Page<VehicleResponse> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID fleetId,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) VehicleStatus status,
            @PageableDefault(sort = "registeredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return vehicleService.findAll(search, fleetId, vehicleType, brand, model, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'TECHNICIAN')")
    @Operation(summary = "Get vehicle by ID (restricted for TECHNICIAN)")
    public Object findById(@PathVariable UUID id, Authentication auth) {
        boolean isTechnician = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TECHNICIAN"));
        return vehicleService.findById(id, isTechnician);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update vehicle")
    public VehicleResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateVehicleRequest request) {
        return vehicleService.update(id, request.licensePlate(), request.brand(), request.model(), request.vehicleType(), request.status(), request.fleetId());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','FLEET_MANAGER', 'TECHNICIAN')")
    @Operation(summary = "Update vehicle status")
    public VehicleResponse patchStatus(@PathVariable UUID id, @Valid @RequestBody PatchVehicleStatusRequest request) {
        return vehicleService.patchStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete vehicle")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/telemetry")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'TECHNICIAN')")
    @Operation(summary = "Get telemetry for a vehicle")
    public Page<TelemetryReadingResponse> findTelemetry(
            @PathVariable UUID id,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return vehicleService.findTelemetry(id, from, to, pageable);
    }

    @GetMapping("/stats/by-type")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Vehicle count by type")
    public Map<VehicleType, Long> getStatsByType() {
        return vehicleService.getStatsByType();
    }
}