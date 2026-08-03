package com.fleetpulse.fleet_api.web.controller;

import com.fleetpulse.fleet_api.domain.entity.AppUser;
import com.fleetpulse.fleet_api.domain.enums.InterventionStatus;
import com.fleetpulse.fleet_api.exception.BusinessRuleViolationException;
import com.fleetpulse.fleet_api.security.CurrentUserResolver;
import com.fleetpulse.fleet_api.service.InterventionService;
import com.fleetpulse.fleet_api.web.dto.request.AssignInterventionRequest;
import com.fleetpulse.fleet_api.web.dto.request.CloseInterventionRequest;
import com.fleetpulse.fleet_api.web.dto.request.CreateInterventionRequest;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/interventions")
@RequiredArgsConstructor
public class InterventionController {

    private final InterventionService interventionService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @Operation(summary = "Create a preventive intervention")
    public ResponseEntity<InterventionResponse> create(@Valid @RequestBody CreateInterventionRequest request) {
        InterventionResponse response = interventionService.create(
                request.vehicleId(),
                request.technicianId(),
                request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER')")
    @Operation(summary = "List interventions with filters")
    public Page<InterventionResponse> findAll(
            @RequestParam(required = false) InterventionStatus status,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) UUID technicianId,
            @PageableDefault(sort = "openedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return interventionService.findAll(status, vehicleId, technicianId, pageable);
    }

    @GetMapping("/by-alert")
    @PreAuthorize("hasAnyRole('ADMIN','FLEET_MANAGER')")
    @Operation(summary = "List interventions related to a specific alert with filters")
    public Page<InterventionResponse> findAllByAlert(
            @RequestParam UUID alertId,
            @RequestParam(required = false) InterventionStatus status,
            @RequestParam(required = false) UUID technicianId,
            @PageableDefault(sort = "openedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return interventionService.findAllByAlert(alertId, status, technicianId, pageable);
    }
    @GetMapping("/assigned-to-me")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "List interventions assigned to current technician")
    public Page<InterventionResponse> findAssignedToMe(
            @RequestParam(required = false) InterventionStatus status,
            @PageableDefault(sort = "openedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserResolver.resolve(jwt);
        return interventionService.findAssignedToMe(currentUser.getId(), status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER', 'TECHNICIAN')")
    @Operation(summary = "Get intervention by ID")
    public InterventionResponse findById(@PathVariable UUID id,
                                          @AuthenticationPrincipal Jwt jwt) {
        InterventionResponse response = interventionService.findById(id);
        String role = jwt.getClaim("realm_access") != null
                ? ((List<String>) ((Map<String, Object>) jwt.getClaim("realm_access")).get("roles")).stream()
                .filter(r -> r.equals("ADMIN") || r.equals("FLEET_MANAGER") || r.equals("TECHNICIAN"))
                .findFirst().orElse("")
                : "";
        if ("TECHNICIAN".equals(role)
                && (response.technician() == null
                || !response.technician().id().equals(currentUserResolver.resolve(jwt).getId()))) {
            throw new BusinessRuleViolationException("Intervention not assigned to you");
        }
        return response;
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @Operation(summary = "Assign intervention to a technician")
    public InterventionResponse assign(@PathVariable UUID id,
                                        @Valid @RequestBody AssignInterventionRequest request) {
        return interventionService.assign(id, request.technicianId());
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Start intervention (must be assigned)")
    public InterventionResponse start(@PathVariable UUID id,
                                       @AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserResolver.resolve(jwt);
        return interventionService.start(id, currentUser);
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Close intervention with report")
    public InterventionResponse close(@PathVariable UUID id,
                                       @Valid @RequestBody CloseInterventionRequest request,
                                       @AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserResolver.resolve(jwt);
        return interventionService.close(id, request.technicianReport(), currentUser);
    }
}