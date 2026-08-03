package com.fleetpulse.fleet_api.specification;

import com.fleetpulse.fleet_api.domain.entity.Alert;
import com.fleetpulse.fleet_api.domain.enums.AlertStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public class AlertSpecifications {

    public static Specification<Alert> statusIn(List<AlertStatus> statuses) {
        return (root, query, cb) -> statuses == null || statuses.isEmpty()
                ? null
                : root.get("status").in(statuses);
    }

    public static Specification<Alert> hasVehicle(UUID vehicleId) {
        return (root, query, cb) -> vehicleId == null
                ? null
                : cb.equal(root.get("vehicle").get("id"), vehicleId);
    }

    public static Specification<Alert> acknowledgedBy(String keycloakId) {
        return (root, query, cb) -> keycloakId == null
                ? null
                : cb.equal(root.get("acknowledgedBy").get("keycloakId"), keycloakId);
    }
}