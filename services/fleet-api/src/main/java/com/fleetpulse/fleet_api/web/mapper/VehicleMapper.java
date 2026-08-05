package com.fleetpulse.fleet_api.web.mapper;

import com.fleetpulse.fleet_api.domain.entity.Vehicle;
import com.fleetpulse.fleet_api.web.dto.response.VehicleResponse;
import com.fleetpulse.fleet_api.web.dto.response.VehicleRestrictedResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "fleetId", source = "fleet.id")
    @Mapping(target = "fleetName", source = "fleet.name")
    VehicleResponse toResponse(Vehicle vehicle);

    VehicleRestrictedResponse toRestrictedResponse(Vehicle vehicle);

}