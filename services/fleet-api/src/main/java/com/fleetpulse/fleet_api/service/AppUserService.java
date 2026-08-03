package com.fleetpulse.fleet_api.service;

import com.fleetpulse.fleet_api.domain.entity.AppUser;
import com.fleetpulse.fleet_api.domain.enums.UserRole;
import com.fleetpulse.fleet_api.domain.enums.UserStatus;
import com.fleetpulse.fleet_api.exception.BusinessRuleViolationException;
import com.fleetpulse.fleet_api.exception.ResourceNotFoundException;
import com.fleetpulse.fleet_api.repository.AppUserRepository;
import com.fleetpulse.fleet_api.specification.AppUserSpecifications;
import com.fleetpulse.fleet_api.web.dto.response.UserResponse;
import com.fleetpulse.fleet_api.web.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final KeycloakUserProvisioningService keycloakService;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse create(String fullName, String email, UserRole role) {

        if (appUserRepository.existsByEmail(email)) {
            throw new BusinessRuleViolationException("Email already exists: " + email);
        }

        String tempPassword = "TempPass123!";
        String keycloakId;

        try {
            keycloakId = keycloakService.createUser(email, fullName, tempPassword);
        } catch (Exception e) {
            log.error("Failed to create Keycloak user. email={}, fullName={}", email, fullName, e);
            throw new RuntimeException("Failed to provision Keycloak user: " + e.getMessage(), e);
        }

        try {
            keycloakService.updateUserRole(keycloakId, role);
        } catch (Exception e) {
            keycloakService.disableUser(keycloakId);
            throw new RuntimeException("Failed to assign role in Keycloak: " + e.getMessage(), e);
        }

        try {
            AppUser user = new AppUser();
            user.setKeycloakId(keycloakId);
            user.setEmail(email);
            user.setFullName(fullName);
            user.setRole(role);
            user.setStatus(UserStatus.ENABLED);

            user = appUserRepository.save(user);
            return userMapper.toResponse(user);

        } catch (Exception e) {
            keycloakService.disableUser(keycloakId);
            throw new RuntimeException("Failed to save local user, Keycloak account disabled: " + e.getMessage(), e);
        }
    }

    public Page<UserResponse> findAll(String search, UserRole role, UserStatus status, Pageable pageable) {
        Specification<AppUser> spec = Specification
                .where(AppUserSpecifications.nameOrEmailContains(search))
                .and(AppUserSpecifications.hasRole(role))
                .and(AppUserSpecifications.hasStatus(status));
        return appUserRepository.findAll(spec, pageable)
                .map(userMapper::toResponse);
    }

    public UserResponse findById(UUID id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", id));
        return userMapper.toResponse(user);
    }
    @Transactional
    public UserResponse updateRole(UUID id, UserRole newRole) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", id));
        if (user.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN) {
            long adminCount = appUserRepository.countByRole(UserRole.ADMIN);
            if (adminCount <= 1) {
                throw new BusinessRuleViolationException("Cannot demote the last ADMIN user");
            }
        }
        keycloakService.updateUserRole(user.getKeycloakId(), newRole);
        user.setRole(newRole);
        user = appUserRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Transactional
    public void disable(UUID id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", id));
        keycloakService.disableUser(user.getKeycloakId());
        user.setStatus(UserStatus.DISABLED);
        appUserRepository.save(user);
    }

    @Transactional
    public void enable(UUID id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", id));
        keycloakService.enableUser(user.getKeycloakId());
        user.setStatus(UserStatus.ENABLED);
        appUserRepository.save(user);
    }
}