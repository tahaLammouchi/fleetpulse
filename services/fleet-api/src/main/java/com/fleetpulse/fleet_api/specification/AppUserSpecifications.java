package com.fleetpulse.fleet_api.specification;

import com.fleetpulse.fleet_api.domain.entity.AppUser;
import com.fleetpulse.fleet_api.domain.enums.UserRole;
import com.fleetpulse.fleet_api.domain.enums.UserStatus;
import org.springframework.data.jpa.domain.Specification;

public class AppUserSpecifications {

    public static Specification<AppUser> nameOrEmailContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return null;
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
            );
        };
    }

    public static Specification<AppUser> hasRole(UserRole role) {
        return (root, query, cb) -> role == null ? null : cb.equal(root.get("role"), role);
    }

    public static Specification<AppUser> hasStatus(UserStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}