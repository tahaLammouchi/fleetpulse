package com.fleetpulse.fleet_api.web.mapper;

import com.fleetpulse.fleet_api.domain.entity.AppUser;
import com.fleetpulse.fleet_api.domain.entity.Intervention;
import com.fleetpulse.fleet_api.web.dto.response.InterventionResponse;
import com.fleetpulse.fleet_api.web.dto.response.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface InterventionMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "licensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "technician", source = "technician", qualifiedByName = "toSummaryIfPresent")
    @Mapping(target = "alertId", source = "alert.id")
    InterventionResponse toResponse(Intervention intervention);

    @Named("toSummaryIfPresent")
    default UserSummaryResponse toSummaryIfPresent(AppUser user) {
        if (user == null) return null;
        return new UserSummaryResponse(user.getId(), user.getFullName());
    }
}