package com.fleetpulse.fleet_api.web.mapper;

import com.fleetpulse.fleet_api.domain.entity.VehicleImage;
import com.fleetpulse.fleet_api.web.dto.response.VehicleImageResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleImageMapper {

    VehicleImageResponse toResponse(VehicleImage vehicleImage);

}
