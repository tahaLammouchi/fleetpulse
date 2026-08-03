package com.fleetpulse.fleet_api.web.mapper;

import com.fleetpulse.fleet_api.domain.entity.AlertThreshold;
import com.fleetpulse.fleet_api.web.dto.response.AlertThresholdResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AlertThresholdMapper {
    AlertThresholdResponse toResponse(AlertThreshold threshold);
}