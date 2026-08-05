package com.fleetpulse.fleet_api.domain.entity;

import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "vehicle", indexes = {
        @Index(name = "idx_vehicle_fleet", columnList = "fleet_id"),
        @Index(name = "idx_vehicle_type_status", columnList = "vehicle_type, status"),
        @Index(name = "idx_vehicle_brand_model", columnList = "brand, model")
})
public class Vehicle extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fleet_id", nullable = false)
    private Fleet fleet;

    @Column(unique = true, nullable = false, length = 20)
    private String licensePlate;

    private String brand;
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Column(nullable = false)
    private LocalDateTime registeredAt;
}
