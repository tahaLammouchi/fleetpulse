package com.fleetpulse.fleet_api.specification;

import com.fleetpulse.fleet_api.domain.entity.Vehicle;
import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class VehicleSpecifications {

    public static Specification<Vehicle> hasFleet(UUID fleetId) {
        return (root, query, cb) -> fleetId == null
                ? null
                : cb.equal(root.get("fleet").get("id"), fleetId);
    }

    public static Specification<Vehicle> plateContains(String search) {
        return (root, query, cb) -> search == null || search.isBlank()
                ? null
                : cb.like(cb.lower(root.get("licensePlate")), "%" + search.toLowerCase() + "%");
    }

    public static Specification<Vehicle> hasBrand(String brand) {
        return (root, query, cb) -> brand == null || brand.isBlank()
                ? null
                : cb.equal(cb.lower(root.get("brand")), brand.toLowerCase());
    }

    public static Specification<Vehicle> hasModel(String model) {
        return (root, query, cb) -> model == null || model.isBlank()
                ? null
                : cb.equal(cb.lower(root.get("model")), model.toLowerCase());
    }

    public static Specification<Vehicle> hasVehicleType(VehicleType vehicleType) {
        return (root, query, cb) -> vehicleType == null
                ? null
                : cb.equal(root.get("vehicleType"), vehicleType);
    }

    public static Specification<Vehicle> hasStatus(VehicleStatus status) {
        return (root, query, cb) -> status == null
                ? null
                : cb.equal(root.get("status"), status);
    }
}