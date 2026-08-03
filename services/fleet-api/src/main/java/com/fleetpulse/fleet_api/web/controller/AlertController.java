package com.fleetpulse.fleet_api.web.controller;

import com.fleetpulse.fleet_api.domain.entity.AppUser;
import com.fleetpulse.fleet_api.domain.enums.AlertStatus;
import com.fleetpulse.fleet_api.security.CurrentUserResolver;
import com.fleetpulse.fleet_api.service.AlertService;
import com.fleetpulse.fleet_api.service.InterventionService;
import com.fleetpulse.fleet_api.web.dto.request.CreateAlertRequest;
import com.fleetpulse.fleet_api.web.dto.request.CreateInterventionRequest;
import com.fleetpulse.fleet_api.web.dto.response.AlertResponse;
import com.fleetpulse.fleet_api.web.dto.response.InterventionResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final InterventionService interventionService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER')")
    @Operation(summary = "List alerts with filters")
    public Page<AlertResponse> findAll(
            @RequestParam(required = false) List<AlertStatus> status,
            @RequestParam(required = false) UUID vehicleId,
            @PageableDefault(sort = "triggeredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return alertService.findAll(status, vehicleId, pageable);
    }

    @GetMapping("/acknowledged")
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @Operation(summary = "List alerts acknowledged by specific fleet manager")
    public Page<AlertResponse> findAcknowledgedByFleetManager(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) List<AlertStatus> status,
            @RequestParam(required = false) UUID vehicleId,
            @PageableDefault(sort = "triggeredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AppUser currentUser = currentUserResolver.resolve(jwt);
        return alertService.findAcknowledgedByFleetManager(currentUser, status, vehicleId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER')")
    @Operation(summary = "Get alert by ID")
    public AlertResponse findById(@PathVariable UUID id) {
        return alertService.findById(id);
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @Operation(summary = "Acknowledge an alert")
    public AlertResponse acknowledge(@PathVariable UUID id,
                                      @AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserResolver.resolve(jwt);
        return alertService.acknowledge(id, currentUser);
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('FLEET_MANAGER')")
    @Operation(summary = "Resolve an acknowledged alert")
    public AlertResponse resolve(@PathVariable UUID id,
                                 @AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserResolver.resolve(jwt);
        return alertService.resolve(id, currentUser);
    }

    @PostMapping("/{alertId}/interventions")
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @Operation(summary = "Create intervention from alert")
    public ResponseEntity<InterventionResponse> createIntervention(
            @PathVariable UUID alertId,
            @Valid @RequestBody CreateInterventionRequest request) {
        var response = interventionService.createFromAlert(alertId, request.technicianId(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}