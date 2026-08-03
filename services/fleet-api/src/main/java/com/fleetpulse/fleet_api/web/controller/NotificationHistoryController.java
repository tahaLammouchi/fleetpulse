package com.fleetpulse.fleet_api.web.controller;

import com.fleetpulse.fleet_api.domain.enums.NotificationStatus;
import com.fleetpulse.fleet_api.service.NotificationHistoryService;
import com.fleetpulse.fleet_api.web.dto.request.CreateNotificationRequest;
import com.fleetpulse.fleet_api.web.dto.response.NotificationHistoryResponse;
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
@RequestMapping("/api/notifications-history")
@RequiredArgsConstructor
public class NotificationHistoryController {

    private final NotificationHistoryService notificationHistoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FLEET_MANAGER')")
    @Operation(summary = "List notification history")
    public Page<NotificationHistoryResponse> findAll(
            @RequestParam(required = false) UUID alertId,
            @RequestParam(required = false) NotificationStatus status,
            @PageableDefault(sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return notificationHistoryService.findAll(alertId, status, pageable);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_service')")
    @Operation(summary = "Internal: create notification history")
    public ResponseEntity<NotificationHistoryResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
        NotificationHistoryResponse response = notificationHistoryService.create(
                request.alertId(), request.sentTo(), request.channel(), request.status());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}