package com.fleetpulse.fleet_api.web.dto.request;

import com.fleetpulse.fleet_api.domain.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record PatchUserRoleRequest(
        @NotNull UserRole role
) {}