package com.fleetpulse.fleet_api.web.mapper;

import com.fleetpulse.fleet_api.domain.entity.AppUser;
import com.fleetpulse.fleet_api.web.dto.response.UserResponse;
import com.fleetpulse.fleet_api.web.dto.response.UserSummaryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(AppUser user);

    UserSummaryResponse toSummary(AppUser user);
}