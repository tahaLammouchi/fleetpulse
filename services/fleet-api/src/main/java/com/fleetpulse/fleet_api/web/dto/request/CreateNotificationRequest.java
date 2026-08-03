package com.fleetpulse.fleet_api.web.dto.request;

import com.fleetpulse.fleet_api.domain.enums.NotificationChannel;
import com.fleetpulse.fleet_api.domain.enums.NotificationStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateNotificationRequest(
        @NotNull UUID alertId,
        @NotNull String sentTo,
        @NotNull NotificationChannel channel,
        @NotNull NotificationStatus status
) {}