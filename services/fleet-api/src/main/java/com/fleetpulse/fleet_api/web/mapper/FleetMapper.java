package com.fleetpulse.fleet_api.web.mapper;

import com.fleetpulse.fleet_api.domain.entity.Fleet;
import com.fleetpulse.fleet_api.web.dto.response.FleetResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FleetMapper {

    @Mapping(target = "vehicleCount", ignore = true)
    FleetResponse toResponse(Fleet fleet);

    default FleetResponse toResponseWithCount(Fleet fleet, long vehicleCount) {
        return new FleetResponse(
                fleet.getId(),
                fleet.getName(),
                vehicleCount,
                fleet.getCreatedAt(),
                fleet.getUpdatedAt()
        );
    }
}