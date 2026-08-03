package com.fleetpulse.fleet_api.specification;

import com.fleetpulse.fleet_api.domain.entity.NotificationHistory;
import com.fleetpulse.fleet_api.domain.enums.NotificationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class NotificationHistorySpecifications {

    public static Specification<NotificationHistory> hasAlert(UUID alertId) {
        return (root, query, cb) -> alertId == null
                ? null
                : cb.equal(root.get("alert").get("id"), alertId);
    }

    public static Specification<NotificationHistory> hasStatus(NotificationStatus status) {
        return (root, query, cb) -> status == null
                ? null
                : cb.equal(root.get("status"), status);
    }
}