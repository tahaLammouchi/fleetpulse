package com.fleetpulse.fleet_api.web.controller;

import com.fleetpulse.fleet_api.domain.enums.UserRole;
import com.fleetpulse.fleet_api.domain.enums.UserStatus;
import com.fleetpulse.fleet_api.service.AppUserService;
import com.fleetpulse.fleet_api.web.dto.request.CreateUserRequest;
import com.fleetpulse.fleet_api.web.dto.request.PatchUserRoleRequest;
import com.fleetpulse.fleet_api.web.dto.response.UserResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AppUserService appUserService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user (provisions Keycloak + local DB)")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = appUserService.create(
                request.fullName(), request.email(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users with filters")
    public Page<UserResponse> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return appUserService.findAll(search, role, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID")
    public UserResponse findById(@PathVariable UUID id) {
        return appUserService.findById(id);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user role (syncs Keycloak)")
    public UserResponse updateRole(@PathVariable UUID id, @Valid @RequestBody PatchUserRoleRequest request) {
        return appUserService.updateRole(id, request.role());
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disable user (Keycloak + local)")
    public ResponseEntity<Void> disable(@PathVariable UUID id) {
        appUserService.disable(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable user (Keycloak + local)")
    public ResponseEntity<Void> enable(@PathVariable UUID id) {
        appUserService.enable(id);
        return ResponseEntity.ok().build();
    }
}