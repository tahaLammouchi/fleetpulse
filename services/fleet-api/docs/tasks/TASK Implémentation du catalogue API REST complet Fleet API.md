# TASK — Implémentation du catalogue API REST complet (Fleet API)

## Contexte

Tu es un architecte Spring Boot senior. Le projet FleetPulse est à l'état suivant :
- Les entités JPA sont finalisées (Fleet, Vehicle, AppUser, Alert, AlertThreshold, Intervention, NotificationHistory) + tous les enums
- Les migrations Flyway V1 à V7 sont appliquées (tables relationnelles + hypertables TimescaleDB)
- Keycloak est configuré (realm `fleetpulse`, rôles ADMIN/FLEET_MANAGER/TECHNICIAN, client `fleetpulse-frontend` public, client `fleet-api` confidentiel avec service account)
- `spring-boot-starter-oauth2-resource-server` est déjà dans le pom.xml

Ta mission : implémenter l'intégralité du catalogue d'API REST ci-dessous, avec la sécurité, le filtrage, la pagination et l'intégration Keycloak déjà décidés dans ce projet.

## Périmètre de la tâche

Génère, pour chaque ressource, l'ensemble complet des couches : Controller, Service, Repository (si absent), DTO (request/response), Mapper (MapStruct), Specification (si filtrage combiné requis), et la configuration de sécurité associée.

Ne modifie AUCUNE entité JPA ni AUCUNE migration Flyway existante — elles sont figées. Si tu détectes une incohérence entre le catalogue ci-dessous et les entités/migrations existantes, signale-la dans ton résumé final au lieu de modifier silencieusement l'un ou l'autre.

## RÈGLE TRANSVERSALE — Sécurité Keycloak

1. Configure le `JwtAuthenticationConverter` pour extraire les rôles depuis le claim `realm_access.roles` du JWT Keycloak, avec le préfixe `ROLE_` (nécessaire pour que `hasRole('ADMIN')` fonctionne). Place cette config dans `security/SecurityConfig.java`.

2. Applique `@PreAuthorize` sur CHAQUE endpoint, avec les rôles exacts spécifiés dans le catalogue ci-dessous — jamais un accès plus large que ce qui est documenté.

3. Pour tout endpoint où l'identité de l'utilisateur courant doit être utilisée (ex: qui acquitte une alerte, qui démarre une intervention, "mes interventions"), extrais-la TOUJOURS depuis `@AuthenticationPrincipal Jwt jwt` (via `jwt.getSubject()` = keycloak_id), puis résous l'`AppUser` local correspondant via `appUserRepository.findByKeycloakId(...)`. Ne JAMAIS accepter un identifiant d'utilisateur en paramètre de requête ou en body pour ce genre d'action — c'est une règle de sécurité non négociable de ce projet.

4. Pour les endpoints marqués "interne, service-to-service" (POST /api/alerts, POST /api/telemetry, POST /api/anomaly-scores, POST /api/notifications-history, POST /api/alert-thresholds), protège-les via une vérification de scope/client (`hasAuthority('SCOPE_service')` ou équivalent selon la configuration du client credentials Keycloak) plutôt que par un rôle utilisateur humain. Si la configuration exacte du client service n'existe pas encore, crée le bean de sécurité correspondant et signale dans le résumé que la configuration Keycloak console (client credentials) doit être faite manuellement.

5. Pour UC-001/002/003 (gestion des comptes), crée un `KeycloakUserProvisioningService` utilisant le SDK `keycloak-admin-client` (ajoute la dépendance si absente), avec un `Keycloak` bean configuré en `CLIENT_CREDENTIALS` grant type pointant sur le client `fleet-api`. Ce service doit exposer : createUser, updateUserRole, disableUser. En cas d'échec de la création locale `app_user` APRÈS succès de la création Keycloak, supprime le compte Keycloak créé (compensation manuelle) pour éviter un compte orphelin.

## RÈGLE TRANSVERSALE — Filtrage, recherche, pagination

1. Pour tout endpoint GET listant une ressource (`GET /api/fleets`, `/api/vehicles`, `/api/users`, `/api/alerts`, `/api/interventions`, `/api/notifications-history`), implémente le filtrage via `JpaSpecificationExecutor<T>` sur le repository correspondant, avec une classe `XxxSpecifications` regroupant une méthode statique par critère filtrable, suivant EXACTEMENT ce pattern :

```java
public static Specification<Vehicle> hasFleet(UUID fleetId) {
    return (root, query, cb) -> fleetId == null ? null : cb.equal(root.get("fleet").get("id"), fleetId);
}

public static Specification<Vehicle> plateContains(String search) {
    return (root, query, cb) -> search == null ? null 
        : cb.like(cb.lower(root.get("licensePlate")), "%" + search.toLowerCase() + "%");
}
```

Chaque critère doit retourner `null` si le paramètre n'est pas fourni (pour être ignoré par l'assemblage `.and()`), jamais lever d'exception ni générer une condition vide.

2. Assemble les critères dans le service avec `Specification.where(...).and(...)`, uniquement pour les critères effectivement présents dans les paramètres de requête reçus.

3. Utilise `Pageable` (via `@PageableDefault`) sur tous les endpoints de liste, avec un tri par défaut cohérent par ressource : `Vehicle` → `registeredAt DESC`, `Alert` → `triggeredAt DESC`, `Intervention` → `openedAt DESC`, `NotificationHistory` → `sentAt DESC`, `Fleet`/`AppUser` → `createdAt DESC`.

4. Retourne systématiquement `Page<XxxResponse>` (jamais `List<>`) pour les endpoints paginés — sauf exceptions explicitement listées comme `List<>` dans le catalogue (`GET /api/users/technicians`, `GET /api/alert-thresholds`).

## RÈGLE TRANSVERSALE — DTO et validation

1. Ne JAMAIS exposer une entité JPA directement dans un controller — toujours passer par un DTO (records Java), avec Mapper MapStruct dédié par ressource.
2. Toute référence à un utilisateur dans une réponse (ex: `acknowledgedBy`, `technician`) doit utiliser un DTO restreint `UserSummaryResponse` (id, fullName uniquement) — jamais l'entité `AppUser` complète (ne doit jamais exposer `keycloakId`).
3. Applique Bean Validation (`@NotNull`, `@NotBlank`, `@Email`, etc.) sur tous les DTO de requête, cohérente avec les contraintes NOT NULL/CHECK définies dans les migrations Flyway existantes.
4. Toute violation de règle métier (transition de statut invalide, alerte déjà acquittée, technicien non conforme au rôle attendu) doit lever une exception métier dédiée (`BusinessRuleViolationException`) traitée par un `GlobalExceptionHandler` déjà en place ou à créer, retournant les codes HTTP exacts spécifiés dans le catalogue (409, 400, 403, 404).

## CATALOGUE COMPLET À IMPLÉMENTER

### 1. Fleet
- POST /api/fleets — ADMIN — body {name} — 201/400
- GET /api/fleets — ADMIN, FLEET_MANAGER — search, page, size, sort — 200 Page<FleetResponse>
- GET /api/fleets/{id} — ADMIN, FLEET_MANAGER — 200/404
- PUT /api/fleets/{id} — ADMIN — body {name} — 200/404/400
- DELETE /api/fleets/{id} — ADMIN — 204/409 (RESTRICT si véhicules rattachés)
- GET /api/fleets/{id}/vehicles — ADMIN, FLEET_MANAGER — page, size, sort — 200 Page<VehicleResponse>
- GET /api/fleets/stats — ADMIN — 200 FleetStatsResponse (nb flottes, top flottes par nb véhicules)

### 2. Vehicle
- POST /api/vehicles — ADMIN — body {fleetId, licensePlate, model, vehicleType} — 201/400/409 (plaque dupliquée)
- GET /api/vehicles — ADMIN, FLEET_MANAGER — search (plaque), fleetId, vehicleType, status, page, size, sort — 200 Page<VehicleResponse>
- GET /api/vehicles/{id} — ADMIN, FLEET_MANAGER, TECHNICIAN — retourne VehicleResponse complet pour ADMIN/FLEET_MANAGER, VehicleRestrictedResponse (champs sensibles masqués) pour TECHNICIAN — 200/404
- PUT /api/vehicles/{id} — ADMIN — body {model, vehicleType} — 200/404/400
- PATCH /api/vehicles/{id}/status — ADMIN — body {status} — 200/404/400 si transition invalide
- DELETE /api/vehicles/{id} — ADMIN — 204/409 (RESTRICT si alertes/interventions existantes)
- GET /api/vehicles/{id}/telemetry — ADMIN, FLEET_MANAGER, TECHNICIAN — from, to (obligatoires), page, size — 200 Page<TelemetryReadingResponse>/400 si from>to. Utilise JdbcTemplate ou requête native pour interroger l'hypertable telemetry_readings, JAMAIS d'entité JPA.
- GET /api/vehicles/stats/by-type — ADMIN — 200 Map<VehicleType, Long>

### 3. AppUser
- POST /api/users — ADMIN — body {fullName, email, role} — 201 UserResponse (avec mot de passe temporaire généré) /409 (email existant)/502 (échec Keycloak) — provisionne Keycloak PUIS app_user
- GET /api/users — ADMIN — search (nom/email), role, page, size, sort — 200 Page<UserResponse>
- GET /api/users/{id} — ADMIN — 200/404
- PATCH /api/users/{id}/role — ADMIN — body {role} — 200/409 si tentative de rétrograder le dernier ADMIN — synchronise Keycloak ET app_user
- PATCH /api/users/{id}/disable — ADMIN — 200/404 — désactive côté Keycloak, conserve l'historique local
- GET /api/users/technicians — ADMIN, FLEET_MANAGER — 200 List<UserResponse> (role=TECHNICIAN uniquement)

### 4. Alert
- GET /api/alerts — ADMIN, FLEET_MANAGER — status (liste), vehicleId, page, size, sort (défaut triggeredAt desc) — 200 Page<AlertResponse>
- GET /api/alerts/{id} — ADMIN, FLEET_MANAGER — 200 AlertResponse (inclut score, véhicule, historique)/404
- PATCH /api/alerts/{id}/acknowledge — FLEET_MANAGER — AUCUN body, identité extraite du JWT — 200 AlertResponse/409 si status != NEW/404
- PATCH /api/alerts/{id}/resolve — FLEET_MANAGER — 200/409 si pas acquittée au préalable (status != ACKNOWLEDGED)
- POST /api/alerts/{alertId}/interventions — FLEET_MANAGER — body {description} — vehicleId déduit de l'alerte — 201/404/409 si alerte encore NEW (pas acquittée)
- POST /api/alerts (interne, service-to-service) — body {vehicleId, anomalyScoreValue, modelVersion, triggeredAt} — 201

### 5. AlertThreshold
- GET /api/alert-thresholds — ADMIN — vehicleType, modelVersion (optionnels) — 200 List<AlertThresholdResponse>
- POST /api/alert-thresholds (interne/technique) — body {vehicleType nullable, modelVersion, thresholdValue} — 201/409 si couple déjà existant

### 6. Intervention
- POST /api/interventions — FLEET_MANAGER — body {vehicleId, description} — 201 (intervention préventive, alertId=null)
- GET /api/interventions — ADMIN, FLEET_MANAGER — status, vehicleId, technicianId, page, size, sort — 200 Page<InterventionResponse>
- GET /api/interventions/assigned-to-me — TECHNICIAN — status (optionnel), page, size — 200 Page<InterventionResponse> — technicianId extrait du JWT, JAMAIS en paramètre
- GET /api/interventions/{id} — ADMIN, FLEET_MANAGER, TECHNICIAN (uniquement si assignée à lui — vérifier ownership via JWT) — 200/403/404
- PATCH /api/interventions/{id}/assign — FLEET_MANAGER — body {technicianId} — 200/400 si l'utilisateur ciblé n'a pas role=TECHNICIAN
- PATCH /api/interventions/{id}/start — TECHNICIAN (assigné uniquement, vérifié via JWT) — 200/403 si non assigné/409 si statut != OPEN
- PATCH /api/interventions/{id}/close — TECHNICIAN (assigné uniquement) — body {technician_report} obligatoire — 200/400 si vide/409 si statut != IN_PROGRESS — EFFET DE BORD : si intervention.alert != null, appeler alert.resolve() automatiquement

### 7. NotificationHistory
- GET /api/notifications-history — ADMIN, FLEET_MANAGER — alertId, status, page, size, sort (défaut sentAt desc) — 200 Page<NotificationHistoryResponse>
- POST /api/notifications-history (interne, service-to-service) — body {alertId, sentTo, channel, status} — 201

### 8. Endpoints internes (hypertables, consumers)
- POST /api/telemetry (interne, non exposé via Kong) — insertion dans l'hypertable telemetry_readings via JdbcTemplate
- POST /api/anomaly-scores (interne, non exposé via Kong) — insertion dans l'hypertable anomaly_scores via JdbcTemplate

## LIVRABLE ATTENDU

1. Implémente toutes les couches pour chaque ressource, dans l'arborescence existante du projet (web/controller, web/dto/request, web/dto/response, web/mapper, domain/service, repository, security).
2. Documente chaque controller avec les annotations springdoc-openapi (@Operation, @ApiResponse) reflétant les rôles requis et les codes de réponse du catalogue.
3. Termine par un résumé sous forme de tableau : Ressource | Endpoints implémentés | Rôles Keycloak appliqués | Filtrage/Specification créé (oui/non) | Anomalie ou incohérence détectée avec l'existant.
4. Ne génère AUCUN test à ce stade — cette tâche se concentre uniquement sur l'implémentation. Les tests feront l'objet d'une tâche séparée.