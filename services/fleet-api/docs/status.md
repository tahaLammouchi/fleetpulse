# FleetPulse — Project Status

> Last updated: 2026-08-03

## Overview

FleetPulse is a fleet monitoring platform built on Spring Boot 4 + PostgreSQL/TimescaleDB, with RabbitMQ for inter-service messaging and Keycloak for authentication. This document tracks the implementation status of the `fleet-api` service.

---

## Tech Stack

| Component | Version | Status |
|---|---|---|
| Java | 21 | Ready |
| Spring Boot | 4.0.7 | Ready |
| PostgreSQL / TimescaleDB | 16 / 2.x | Ready |
| RabbitMQ | 3.13 | Ready |
| Keycloak | 25.0 | Ready |
| Flyway | 10.x (via Spring Boot) | Active |
| Hibernate / JPA | 6.x (via Spring Boot) | `ddl-auto: validate` |
| OpenAPI (SpringDoc) | 2.5.0 | Configured |
| MapStruct | 1.5.5.Final | Active |
| Lombok | via Spring Boot | Active |

---

## Database Schema (Flyway Migrations)

### Relational Tables (V1–V3)

| Migration | Tables | Key Constraints |
|---|---|---|
| `V1__create_enums_and_core_tables.sql` | `fleet`, `app_user`, `vehicle` | CHECK enums, FK `vehicle → fleet` RESTRICT, UQ `keycloak_id`, `email`, `license_plate` |
| `V2__create_alert_and_intervention.sql` | `alert`, `alert_threshold`, `intervention` | CHECK enums, FK `alert → vehicle` RESTRICT, `intervention → vehicle` RESTRICT, `intervention → alert` RESTRICT, nullable `acknowledged_by` / `technician_id` SET NULL, UQ `(vehicle_type, model_version)` + partial index for NULL |
| `V3__create_notification_history.sql` | `notification_history` | FK `notification_history → alert` CASCADE, CHECK enums |

### Hypertables (V4–V6)

| Migration | Table | Policies |
|---|---|---|
| `V4__enable_timescaledb_extension.sql` | Extension activation | — |
| `V5__create_telemetry_hypertable.sql` | `telemetry_readings` | Compression +7d, retention 90d, continuous aggregate `telemetry_hourly` |
| `V6__create_anomaly_scores_hypertable.sql` | `anomaly_scores` | Compression +7d, retention 90d |

---

## Java Domain Model

### Entities (all with `@Getter`/`@Setter` via Lombok)

| Entity | Table | JPA Status |
|---|---|---|
| `DomainEntity` (MappedSuperclass) | — | `id` UUID, `createdAt`, `updatedAt` |
| `Fleet` | `fleet` | `name` |
| `AppUser` | `app_user` | `keycloakId`, `email`, `fullName`, `role` |
| `Vehicle` | `vehicle` | `fleet` (FK), `licensePlate`, `brand`, `model`, `vehicleType`, `status`, `registeredAt` |
| `Alert` | `alert` | `vehicle` (FK), `anomalyScoreValue`, `modelVersion`, `status`, `triggeredAt`, `acknowledgedAt`, `resolvedAt`, `acknowledgedBy` (FK) |
| `AlertThreshold` | `alert_threshold` | `vehicleType`, `modelVersion`, `thresholdValue` |
| `Intervention` | `intervention` | `vehicle` (FK), `technician` (FK), `alert` (FK), `status`, `description`, `technicianReport`, `openedAt`, `closedAt` |
| `NotificationHistory` | `notification_history` | `alert` (FK), `sentTo`, `channel`, `status`, `sentAt` |

### Enums

| Enum | Values |
|---|---|
| `VehicleType` | `VAN`, `TRUCK`, `CAR` |
| `VehicleStatus` | `ACTIVE`, `MAINTENANCE`, `DECOMMISSIONED` |
| `UserRole` | `FLEET_MANAGER`, `TECHNICIAN`, `ADMIN` |
| `AlertStatus` | `NEW`, `ACKNOWLEDGED`, `RESOLVED` |
| `InterventionStatus` | `OPEN`, `IN_PROGRESS`, `CLOSED` |
| `NotificationChannel` | `EMAIL`, `WEBHOOK` |
| `NotificationStatus` | `SENT`, `FAILED` |

---

## Application Configuration

| Setting | Value | Source |
|---|---|---|
| Server port | `8090` | `application.yaml` |
| Database | `jdbc:postgresql://localhost:5432/fleetpulse` | env `DB_USER` / `DB_PASSWORD` |
| JPA ddl-auto | `validate` | `application.yaml` |
| Flyway | enabled | `application.yaml` |
| RabbitMQ | `localhost:5672` | env `RABBITMQ_*` |
| Keycloak JWT issuer | `http://localhost:8081/realms/fleetpulse` | env `KEYCLOAK_ISSUER_URI` |
| Keycloak admin client | `localhost:8081`, realm `fleetpulse`, client `fleet-api` | env `KEYCLOAK_CLIENT_SECRET` |

---

## REST API Catalogue — Implementation Status

Implemented via 8 controllers, 7 services, 7 repositories, 6 specification classes, 7 MapStruct mappers, 15 request DTOs, 11 response DTOs, 3 infrastructure services, and supporting security/exception infrastructure.

### 1. Fleet (`FleetController`)

| Endpoint | Method | Roles | Status |
|---|---|---|---|
| `/api/fleets` | POST | ADMIN | ✅ 201/400 |
| `/api/fleets` | GET | ADMIN, FLEET_MANAGER | ✅ 200, paginated, search filter |
| `/api/fleets/{id}` | GET | ADMIN, FLEET_MANAGER | ✅ 200/404 |
| `/api/fleets/{id}` | PUT | ADMIN | ✅ 200/404/400 |
| `/api/fleets/{id}` | DELETE | ADMIN | ✅ 204/409 (RESTRICT if vehicles exist) |
| `/api/fleets/{id}/vehicles` | GET | ADMIN, FLEET_MANAGER | ✅ 200, paginated |
| `/api/fleets/stats` | GET | ADMIN | ✅ 200 FleetStatsResponse |

### 2. Vehicle (`VehicleController`)

| Endpoint | Method | Roles | Status |
|---|---|---|---|
| `/api/vehicles` | POST | ADMIN | ✅ 201/400/409 |
| `/api/vehicles` | GET | ADMIN, FLEET_MANAGER | ✅ 200, search + filters (fleetId, vehicleType, status) + pagination |
| `/api/vehicles/{id}` | GET | ADMIN, FLEET_MANAGER, TECHNICIAN | ✅ 200/404, role-based response (restricted for TECHNICIAN) |
| `/api/vehicles/{id}` | PUT | ADMIN | ✅ 200/404/400 |
| `/api/vehicles/{id}/status` | PATCH | ADMIN | ✅ 200/404/400 (validated transitions) |
| `/api/vehicles/{id}` | DELETE | ADMIN | ✅ 204/409 (RESTRICT if alerts/interventions) |
| `/api/vehicles/{id}/telemetry` | GET | ADMIN, FLEET_MANAGER, TECHNICIAN | ✅ 200, JdbcTemplate query on `telemetry_readings` |
| `/api/vehicles/stats/by-type` | GET | ADMIN | ✅ 200 Map<VehicleType, Long> |

### 3. AppUser (`UserController` + `KeycloakUserProvisioningService`)

| Endpoint | Method | Roles | Status |
|---|---|---|---|
| `/api/users` | POST | ADMIN | ✅ 201/409/502, Keycloak provision → local, rollback on failure |
| `/api/users` | GET | ADMIN | ✅ 200, search (name/email) + role filter + pagination |
| `/api/users/{id}` | GET | ADMIN | ✅ 200/404 |
| `/api/users/{id}/role` | PATCH | ADMIN | ✅ 200/409 (last ADMIN guard), syncs Keycloak |
| `/api/users/{id}/disable` | PATCH | ADMIN | ✅ 200/404, disables Keycloak + preserves local history |
| `/api/users/technicians` | GET | ADMIN, FLEET_MANAGER | ✅ 200 List, role=TECHNICIAN only |

### 4. Alert (`AlertController`)

| Endpoint | Method | Roles | Status |
|---|---|---|---|
| `/api/alerts` | GET | ADMIN, FLEET_MANAGER | ✅ 200, status + vehicleId filters, pagination |
| `/api/alerts/{id}` | GET | ADMIN, FLEET_MANAGER | ✅ 200/404 |
| `/api/alerts/{id}/acknowledge` | PATCH | FLEET_MANAGER | ✅ 200/409, identity from JWT |
| `/api/alerts/{id}/resolve` | PATCH | FLEET_MANAGER | ✅ 200/409 (must be ACKNOWLEDGED first) |
| `/api/alerts/{alertId}/interventions` | POST | FLEET_MANAGER | ✅ 201/404/409 (alert must not be NEW) |
| `/api/alerts` | POST | SCOPE_service | ✅ 201 internal |

### 5. AlertThreshold (`AlertThresholdController`)

| Endpoint | Method | Roles | Status |
|---|---|---|---|
| `/api/alert-thresholds` | GET | ADMIN | ✅ 200 List, optional vehicleType/modelVersion filters |
| `/api/alert-thresholds` | POST | SCOPE_service | ✅ 201/409 (duplicate check) |

### 6. Intervention (`InterventionController`)

| Endpoint | Method | Roles | Status |
|---|---|---|---|
| `/api/interventions` | POST | FLEET_MANAGER | ✅ 201 preventive (alertId=null) |
| `/api/interventions` | GET | ADMIN, FLEET_MANAGER | ✅ 200, status + vehicleId + technicianId filters, pagination |
| `/api/interventions/assigned-to-me` | GET | TECHNICIAN | ✅ 200, JWT-based ownership |
| `/api/interventions/{id}` | GET | ADMIN, FLEET_MANAGER, TECHNICIAN | ✅ 200/403/404, ownership check for TECHNICIAN |
| `/api/interventions/{id}/assign` | PATCH | FLEET_MANAGER | ✅ 200/400 (target must be TECHNICIAN) |
| `/api/interventions/{id}/start` | PATCH | TECHNICIAN (assigned) | ✅ 200/403/409 (must be OPEN) |
| `/api/interventions/{id}/close` | PATCH | TECHNICIAN (assigned) | ✅ 200/400/409, auto-resolves linked alert |

### 7. NotificationHistory (`NotificationHistoryController`)

| Endpoint | Method | Roles | Status |
|---|---|---|---|
| `/api/notifications-history` | GET | ADMIN, FLEET_MANAGER | ✅ 200, alertId + status filters, pagination |
| `/api/notifications-history` | POST | SCOPE_service | ✅ 201 internal |

### 8. Internal Ingestion (`InternalIngestionController`)

| Endpoint | Method | Roles | Status |
|---|---|---|---|
| `/api/telemetry` | POST | SCOPE_service | ✅ 201, inserts into `telemetry_readings` hypertable via JdbcTemplate |
| `/api/anomaly-scores` | POST | SCOPE_service | ✅ 201, inserts into `anomaly_scores` hypertable via JdbcTemplate |

---

## What's Done

### Infrastructure
- `BusinessRuleViolationException`, `ResourceNotFoundException` — typed exceptions
- `GlobalExceptionHandler` — `@RestControllerAdvice` mapping to 400/404/409/502
- `CurrentUserResolver` — resolves `AppUser` from JWT `subject`
- `OpenApiConfig` — `@OpenAPIDefinition` with bearer JWT security scheme
- MapStruct 1.5.5 + processor in pom.xml (all mappers)
- `@Getter`/`@Setter` Lombok on all 8 entities (needed for mappers/services)

### Repositories (7)
- `FleetRepository`, `VehicleRepository`, `AppUserRepository`, `AlertRepository`, `AlertThresholdRepository`, `InterventionRepository`, `NotificationHistoryRepository`
- All extend `JpaRepository` + `JpaSpecificationExecutor` where filtering is needed

### Specifications (6)
- `FleetSpecifications` (nameContains)
- `VehicleSpecifications` (hasFleet, plateContains, hasVehicleType, hasStatus)
- `AppUserSpecifications` (nameOrEmailContains, hasRole)
- `AlertSpecifications` (statusIn, hasVehicle)
- `InterventionSpecifications` (hasStatus, hasVehicle, hasTechnician, assignedTo)
- `NotificationHistorySpecifications` (hasAlert, hasStatus)

### Services (10)
- `FleetService`, `VehicleService`, `AppUserService`, `AlertService`, `AlertThresholdService`, `InterventionService`, `NotificationHistoryService` — all CRUD
- `KeycloakUserProvisioningService` — provision/update/disable Keycloak users
- `TelemetryIngestionService`, `AnomalyScoreIngestionService` — JdbcTemplate inserts into hypertables

### Mappers (7 MapStruct)
- `FleetMapper`, `VehicleMapper`, `UserMapper`, `AlertMapper`, `AlertThresholdMapper`, `InterventionMapper`, `NotificationHistoryMapper`

### Security
- `JwtAuthenticationConverter` extracts roles from `realm_access.roles` with `ROLE_` prefix
- `@PreAuthorize` on every controller method with exact roles per catalogue
- `SecurityFilterChain` permits health/swagger, protects internal endpoints with `SCOPE_service`
- Read-only internal endpoints require no additional matcher (`.anyRequest().authenticated()`)

---

## Testing

Integration test suite (`src/test/java`) — **68 tests, all green** via `mvnw.cmd clean verify` (failsafe).

### Infrastructure

| Component | Description |
|---|---|
| `AbstractIntegrationTest` | Base class: `PostgreSQLContainer` (TimescaleDB pg16) `.asCompatibleSubstituteFor("postgres")` + `.withReuse(true)`; `@Sql(clean-db.sql)` truncates 9 tables before each test method |
| `clean-db.sql` | `TRUNCATE fleet, app_user, vehicle, alert, alert_threshold, intervention, notification_history, telemetry_readings, anomaly_scores CASCADE` |
| `TestSecurityBeans` | Test double for `CurrentUserResolver`; `@ActiveProfiles("test")` |
| `application-test.yaml` | Hikari timeouts 30000/60000 |
| `testcontainers.properties` | `testcontainers.reuse.enable=true` (+ env var via surefire/failsafe in `pom.xml`) |
| `FleetApiApplicationTests` | `contextLoads` smoke test |

### Test classes

| Class | Focus |
|---|---|
| `controller/FleetControllerIT` | Fleet CRUD, stats, security roles |
| `controller/VehicleControllerIT` | Vehicle CRUD, status transitions, telemetry |
| `controller/AlertControllerIT` | Alert list/get, acknowledge/resolve, create intervention from alert |
| `controller/AlertThresholdControllerIT` | Threshold list/create |
| `controller/InterventionControllerIT` | Intervention create/list, role checks |
| `repository/FleetRepositoryIT` | Repo-level CRUD + filters |
| `repository/VehicleRepositoryIT` | Repo-level CRUD + filters |
| `repository/AlertRepositoryIT` | Delete guards (FK RESTRICT) |
| `repository/AlertThresholdRepositoryIT` | Repo-level CRUD |
| `repository/InterventionRepositoryIT` | Delete with `technician_id` `ON DELETE SET NULL` |
| `repository/AnomalyScoresIT` | Hypertable via JdbcTemplate |
| `repository/TelemetryReadingsIT` | Hypertable via JdbcTemplate |

Controller tests use `@AutoConfigureMockMvc` + `MockMvcWebTestClient.bindTo(mockMvc)` (servlet-compatible `@WithMockUser`), not the reactive `RANDOM_PORT` setup.

---

## What's Not Done

- **Unit tests** — Coverage is integration-only so far; no pure unit tests (`@WebMvcTest`/`@DataJpaTest`) exist yet
- **Messaging consumers** — `messaging/consumer/` and `messaging/publisher/` are empty (RabbitMQ wiring not implemented)
- **Keycloak realm export** — `keycloak/realm-export.json/` is an empty directory; a `fleet-api` confidential client with `CLIENT_CREDENTIALS` grant must be created manually in the Keycloak admin console

---

## Known Issues

1. **Keycloak console setup needed** — The `KeycloakUserProvisioningService` and `SCOPE_service` protection require a `fleet-api` confidential client in Keycloak with `CLIENT_CREDENTIALS` grant type and appropriate service role/scope. This must be done manually.
2. **`CurrentUserResolver` depends on `AppUserRepository.findByKeycloakId()`** — The resolver extracts the subject from the JWT and looks up the local `AppUser`. This requires all users to exist in both Keycloak and the local DB.
3. **`telemetry_readings` and `anomaly_scores`** are never mapped via JPA — accessed exclusively through `JdbcTemplate`. This is intentional (high write throughput design).
4. **`JpaAuditingConfig.java` is an empty stub** — `@EnableJpaAuditing` was moved to `FleetApiApplication`, so the config class is obsolete but not removed.
5. **No `/readyz` health check** — Returns `{"status":"ready"}` unconditionally; RabbitMQ/DB connectivity checks not implemented.

---

## Implementation Summary

| Resource | Endpoints | Roles | Specifications | Status |
|---|---|---|---|---|
| Fleet | 7 | ADMIN, FLEET_MANAGER | Yes | ✅ Complete |
| Vehicle | 8 | ADMIN, FLEET_MANAGER, TECHNICIAN | Yes | ✅ Complete |
| AppUser | 6 | ADMIN | Yes | ✅ Complete |
| Alert | 6 | ADMIN, FLEET_MANAGER, SCOPE_service | Yes | ✅ Complete |
| AlertThreshold | 2 | ADMIN, SCOPE_service | No (List) | ✅ Complete |
| Intervention | 7 | ADMIN, FLEET_MANAGER, TECHNICIAN | Yes | ✅ Complete |
| NotificationHistory | 2 | ADMIN, FLEET_MANAGER, SCOPE_service | Yes | ✅ Complete |
| Internal ingestion | 2 | SCOPE_service | No | ✅ Complete |
| **Total** | **40** | — | — | **✅ All implemented** |
