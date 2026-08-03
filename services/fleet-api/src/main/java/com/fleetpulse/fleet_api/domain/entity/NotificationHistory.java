package com.fleetpulse.fleet_api.domain.entity;

import com.fleetpulse.fleet_api.domain.enums.NotificationChannel;
import com.fleetpulse.fleet_api.domain.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class NotificationHistory extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @Column(nullable = false)
    private String sentTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = false)
    private LocalDateTime sentAt;
}
