package com.fleetpulse.fleet_api.web.mapper;

import com.fleetpulse.fleet_api.domain.entity.NotificationHistory;
import com.fleetpulse.fleet_api.web.dto.response.NotificationHistoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationHistoryMapper {

    @Mapping(target = "alertId", source = "alert.id")
    NotificationHistoryResponse toResponse(NotificationHistory notificationHistory);
}