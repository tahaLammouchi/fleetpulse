package com.fleetpulse.fleet_api.specification;

import com.fleetpulse.fleet_api.domain.entity.Intervention;
import com.fleetpulse.fleet_api.domain.enums.InterventionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class InterventionSpecifications {

    public static Specification<Intervention> hasStatus(InterventionStatus status) {
        return (root, query, cb) -> status == null
                ? null
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Intervention> hasVehicle(UUID vehicleId) {
        return (root, query, cb) -> vehicleId == null
                ? null
                : cb.equal(root.get("vehicle").get("id"), vehicleId);
    }

    public static Specification<Intervention> hasTechnician(UUID technicianId) {
        return (root, query, cb) -> technicianId == null
                ? null
                : cb.equal(root.get("technician").get("id"), technicianId);
    }

    public static Specification<Intervention> assignedTo(UUID technicianId) {
        return (root, query, cb) -> technicianId == null
                ? null
                : cb.equal(root.get("technician").get("id"), technicianId);
    }

    public static Specification<Intervention> hasAlert(UUID alertId) {
        return (root, query, cb) -> alertId == null
                ? null
                : cb.equal(root.get("alert").get("id"), alertId);
    }
}