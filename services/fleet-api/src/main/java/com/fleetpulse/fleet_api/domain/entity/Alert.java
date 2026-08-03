package com.fleetpulse.fleet_api.domain.entity;

import com.fleetpulse.fleet_api.domain.enums.AlertStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(indexes = {
        @Index(name = "idx_alert_vehicle_status", columnList = "vehicle_id, status"),
        @Index(name = "idx_alert_triggered_at", columnList = "triggered_at")
})
public class Alert extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private Double anomalyScoreValue;

    @Column(nullable = false)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @Column(nullable = false)
    private LocalDateTime triggeredAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private AppUser acknowledgedBy;
}