package com.fleetpulse.fleet_api.web.dto.response;

import com.fleetpulse.fleet_api.domain.enums.NotificationChannel;
import com.fleetpulse.fleet_api.domain.enums.NotificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationHistoryResponse(
        UUID id,
        UUID alertId,
        String sentTo,
        NotificationChannel channel,
        NotificationStatus status,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {}