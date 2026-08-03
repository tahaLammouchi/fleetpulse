package com.fleetpulse.fleet_api.web.controller;

import com.fleetpulse.fleet_api.service.FleetService;
import com.fleetpulse.fleet_api.web.dto.request.CreateFleetRequest;
import com.fleetpulse.fleet_api.web.dto.request.UpdateFleetRequest;
import com.fleetpulse.fleet_api.web.dto.response.FleetResponse;
import com.fleetpulse.fleet_api.web.dto.response.FleetStatsResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/fleets")
@RequiredArgsConstructor
public class FleetController {

    private final FleetService fleetService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a fleet", responses = {
            @ApiResponse(responseCode = "201", description = "Fleet created"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<FleetResponse> create(@Valid @RequestBody CreateFleetRequest request) {
        FleetResponse response = fleetService.create(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER')")
    @Operation(summary = "List fleets with optional search")
    public Page<FleetResponse> findAll(
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return fleetService.findAll(search, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER')")
    @Operation(summary = "Get fleet by ID")
    public FleetResponse findById(@PathVariable UUID id) {
        return fleetService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update fleet name")
    public FleetResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateFleetRequest request) {
        return fleetService.update(id, request.name());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete fleet", responses = {
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "409", description = "Fleet has vehicles attached")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fleetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER')")
    @Operation(summary = "List vehicles in a fleet")
    public ResponseEntity<?> findVehicles(@PathVariable UUID id, Pageable pageable) {
        return ResponseEntity.ok(fleetService.findVehiclesByFleetId(id, pageable));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','FLEET_Manager')")
    @Operation(summary = "Fleet statistics")
    public FleetStatsResponse getStats() {
        return fleetService.getStats();
    }
}