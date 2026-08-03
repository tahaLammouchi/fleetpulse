package com.fleetpulse.fleet_api.domain.entity;

import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uq_threshold_type_version",
        columnNames = {"vehicle_type", "model_version"}
))
public class AlertThreshold extends DomainEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private VehicleType vehicleType;

    @Column(nullable = false)
    private String modelVersion;

    @Column(nullable = false)
    private Double thresholdValue;
}
