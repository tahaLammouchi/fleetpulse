package com.fleetpulse.fleet_api.specification;

import com.fleetpulse.fleet_api.domain.entity.Fleet;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class FleetSpecifications {

    public static Specification<Fleet> nameContains(String search) {
        return (root, query, cb) -> search == null || search.isBlank()
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
    }
}