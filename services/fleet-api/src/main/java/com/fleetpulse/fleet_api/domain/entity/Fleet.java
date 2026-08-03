package com.fleetpulse.fleet_api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Fleet extends DomainEntity {

    @Column(nullable = false)
    private String name;
}
