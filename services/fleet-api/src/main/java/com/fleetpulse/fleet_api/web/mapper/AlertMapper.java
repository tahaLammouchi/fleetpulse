package com.fleetpulse.fleet_api.web.mapper;

import com.fleetpulse.fleet_api.domain.entity.Alert;
import com.fleetpulse.fleet_api.domain.entity.AppUser;
import com.fleetpulse.fleet_api.web.dto.response.AlertResponse;
import com.fleetpulse.fleet_api.web.dto.response.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AlertMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "licensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "acknowledgedBy", source = "acknowledgedBy", qualifiedByName = "toSummaryIfPresent")
    AlertResponse toResponse(Alert alert);

    @Named("toSummaryIfPresent")
    default UserSummaryResponse toSummaryIfPresent(AppUser user) {
        if (user == null) return null;
        return new UserSummaryResponse(user.getId(), user.getFullName());
    }
}