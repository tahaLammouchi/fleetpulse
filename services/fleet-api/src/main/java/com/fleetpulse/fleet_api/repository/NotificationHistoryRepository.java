package com.fleetpulse.fleet_api.repository;

import com.fleetpulse.fleet_api.domain.entity.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface NotificationHistoryRepository
        extends JpaRepository<NotificationHistory, UUID>, JpaSpecificationExecutor<NotificationHistory> {
}