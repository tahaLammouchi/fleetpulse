package com.fleetpulse.fleet_api.service;

import com.fleetpulse.fleet_api.domain.enums.UserRole;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserProvisioningService {

    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.admin.realm}")
    private String realm;

    public String createUser(String email, String fullName, String password) {

        UserRepresentation user = new UserRepresentation();
        user.setEmail(email);
        user.setUsername(email);
        user.setFirstName(fullName);
        user.setEnabled(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(true);

        user.setCredentials(List.of(credential));

        RealmResource realmResource = keycloakAdminClient.realm(realm);

        try (Response response = realmResource.users().create(user)) {

            if (response.getStatus() == 201) {
                String userId = response.getLocation()
                        .getPath()
                        .replaceAll(".*/([^/]+)$", "$1");

                log.info("Keycloak user created successfully. email={}, id={}", email, userId);

                return userId;
            }

            String body = response.hasEntity()
                    ? response.readEntity(String.class)
                    : "<empty>";

            throw new RuntimeException(
                    "Failed to create Keycloak user: HTTP "
                            + response.getStatus()
                            + " Response: "
                            + body);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Keycloak user: " + e.getMessage(), e);
        }
    }

    public void updateUserRole(String keycloakId, UserRole newRole) {
        RealmResource realmResource = keycloakAdminClient.realm(realm);
        UserResource userResource = realmResource.users().get(keycloakId);

        List<RoleRepresentation> existingRoles = userResource.roles().realmLevel().listAll();
        userResource.roles().realmLevel().remove(existingRoles);

        String keycloakRoleName = switch (newRole) {
            case ADMIN -> "ADMIN";
            case FLEET_MANAGER -> "FLEET_MANAGER";
            case TECHNICIAN -> "TECHNICIAN";
        };
        RoleRepresentation roleRep = realmResource.roles().get(keycloakRoleName).toRepresentation();
        userResource.roles().realmLevel().add(List.of(roleRep));
    }

    public void disableUser(String keycloakId) {
        RealmResource realmResource = keycloakAdminClient.realm(realm);
        UserResource userResource = realmResource.users().get(keycloakId);
        UserRepresentation user = userResource.toRepresentation();
        user.setEnabled(false);
        userResource.update(user);
    }

    public void enableUser(String keycloakId) {
        RealmResource realmResource = keycloakAdminClient.realm(realm);
        UserResource userResource = realmResource.users().get(keycloakId);
        UserRepresentation user = userResource.toRepresentation();
        user.setEnabled(true);
        userResource.update(user);
    }
}