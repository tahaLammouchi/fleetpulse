package com.fleetpulse.fleet_api.domain.entity;

import com.fleetpulse.fleet_api.domain.enums.InterventionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class Intervention extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private AppUser technician;   // nullable

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id")
    private Alert alert;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterventionStatus status;
    private String description;

    private String technicianReport;

    @Column(nullable = false)
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    public void close(String technicianReport) {
        if (this.status != InterventionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Seule une intervention IN_PROGRESS peut être clôturée");
        }
        if (technicianReport == null || technicianReport.isBlank()) {
            throw new IllegalArgumentException("Le rapport technicien est obligatoire à la clôture");
        }
        this.technicianReport = technicianReport;
        this.status = InterventionStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }
}