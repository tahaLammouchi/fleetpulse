package com.fleetpulse.fleet_api.security;

import com.fleetpulse.fleet_api.domain.entity.AppUser;
import com.fleetpulse.fleet_api.exception.ResourceNotFoundException;
import com.fleetpulse.fleet_api.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final AppUserRepository appUserRepository;

    public AppUser resolve(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return appUserRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", "keycloakId", keycloakId));
    }
}