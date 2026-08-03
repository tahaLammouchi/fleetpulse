# FleetPulse — Global Project Status

> Last updated: 2026-07-27

## Architecture Overview

FleetPulse is a fleet monitoring platform composed of **6 microservices** connected via RabbitMQ:

```
telemetry-simulator (:8005) → ingestion (:8001) → ml-inference (:8002) → alerting (:8003) → notification (:8004)
                                                                                              ↘ fleet-api (:8090, Java)
```

| Service | Language | Port | Role |
|---------|----------|------|------|
| telemetry-simulator | Python 3.12 / FastAPI | 8005 | Generates synthetic telemetry data (pipeline source) |
| ingestion | Python 3.12 / FastAPI | 8001 | Receives and validates telemetry data |
| ml-inference | Python 3.12 / FastAPI | 8002 | Computes anomaly scores |
| alerting | Python 3.12 / FastAPI | 8003 | Generates alerts based on anomaly scores |
| notification | Python 3.12 / FastAPI | 8004 | Sends notifications (pipeline sink) |
| fleet-api | Java 21 / Spring Boot 4 | 8090 | REST API for fleet management |

---

## Infrastructure

All services run via `docker-compose up -d` at the repo root:

| Service | Image | Port(s) | Purpose |
|---------|-------|---------|---------|
| PostgreSQL | timescale/timescaledb:latest-pg16 | 5432 | Primary database + time-series |
| RabbitMQ | rabbitmq:3.13-management | 5672 (AMQP), 15672 (UI) | Inter-service messaging |
| Keycloak | quay.io/keycloak/keycloak:25.0 | 8081 | Authentication / JWT issuer |

Default credentials: `fleetpulse` / `fleetpulse` (dev only).

---

## Shared Contracts

All inter-service message formats are defined as JSON Schemas in `contracts/`:

| Schema File | Used By |
|---|---|
| `telemetry.schema.json` | ingestion (validates incoming telemetry) |
| `anomaly-score.schema.json` | ml-inference (validates anomaly scores) |
| `alert-event.schema.json` | alerting (validates alert events) |

---

## CI Pipeline

| Service | CI File | Status | Notes |
|---------|---------|--------|-------|
| fleet-api | `fleet-api-ci.yml` | Configured | JDK 21, Maven, Checkstyle, spins up TimescaleDB for tests |
| ingestion | `ingestion-ci.yml` | **Empty (0 bytes)** | No CI workflow defined |
| ml-inference | `ml-inference-ci.yml` | Configured | Python 3.12, ruff, pytest (uses tabs) |
| alerting | `alerting-ci.yml` | Configured | Python 3.12, ruff, pytest (uses spaces) |
| notification | `notification-ci.yml` | Configured | Python 3.12, ruff, pytest (uses tabs) |
| telemetry-simulator | `telemetry-simulator-ci.yml` | Configured | Python 3.12, ruff, pytest (uses tabs) |

---

## Per-Service Summary

| Service | Port | App | Health | Config | Schemas | Consumer | Publisher | Dockerfile | Tests | CI |
|---------|------|-----|--------|--------|---------|----------|-----------|------------|-------|----|
| telemetry-simulator | 8005 | ✅ | ✅ | ✅ | ❌ Empty | ❌ Missing | ❌ Missing | ❌ Empty | ❌ Empty | ✅ |
| ingestion | 8001 | ✅ | ✅ | ✅ | ✅ | ❌ Empty | ❌ Empty | ❌ Empty | ❌ Empty | ❌ Empty |
| ml-inference | 8002 | ✅ | ✅ | ✅ | ✅ | ❌ Empty | ❌ Empty | ❌ Empty | ❌ Empty | ✅ |
| alerting | 8003 | ✅ | ✅ | ✅ | ✅ | ❌ Empty | ❌ Empty | ❌ Empty | ❌ Empty | ✅ |
| notification | 8004 | ✅ | ✅ | ✅ | ❌ Empty | ❌ Empty | ❌ Missing | ❌ Empty | ❌ Empty | ✅ |
| fleet-api | 8090 | ✅ | ✅ | ✅ | ✅ | ❌ Empty | ❌ Empty | ✅ | ❌ Empty | ✅ |

---

## Shared Contracts

| Schema File | Loaded By | Status |
|---|---|---|
| `contracts/telemetry.schema.json` | ingestion | Present |
| `contracts/anomaly-score.schema.json` | ml-inference | Present |
| `contracts/alert-event.schema.json` | alerting | Present |

---

## Cross-Cutting Issues

1. **Copy-paste title bug** — All 5 Python services have `title="Ingestion Service"` in `main.py` instead of their own service name.
2. **RabbitMQ wiring not implemented** — `consumer.py`/`publisher.py` are empty stubs (or missing) across all services.
3. **No tests exist** — All `tests/` directories are empty.
4. **Dockerfiles are empty** — All 5 Python services have empty `Dockerfile` stubs.
5. **`/readyz` is a stub** — All services return `{"status": "ready"}` unconditionally; RabbitMQ connectivity checks are not implemented.
6. **`ingestion-ci.yml` is empty** — The only CI file without content (0 bytes).
7. **CI indentation inconsistency** — Some workflows use spaces, others use tabs.
8. **`.gitignore` has markdown code-fence backticks** — First/last lines are `` ``` ``.
9. **No `pyproject.toml` or `ruff.toml`** — ruff uses defaults with no config file.
10. **Keycloak realm export missing** — `keycloak/realm-export.json/` is an empty directory.

---

## Notable Changes

- **fleet-api REST catalogue fully implemented** — 40 endpoints across 8 controllers, 7 services, 7 repositories, 7 MapStruct mappers, 6 specification classes, including Keycloak user provisioning via `keycloak-admin-client`. Compilation verified (`./mvnw compile`). See `services/fleet-api/docs/status.md` for details.

## Next Steps (Recommended Order)

1. **Fix copy-paste title bug** in all 5 Python `main.py` files
2. **Implement RabbitMQ wiring** — `consumer.py`/`publisher.py` for each service
3. **Write tests** — start with unit tests, then integration tests with Testcontainers
4. **Populate Dockerfiles** — container build instructions
5. **Fix `ingestion-ci.yml`** — add CI workflow content
6. **Implement `/readyz`** — wire RabbitMQ/DB connectivity checks
7. **Keycloak realm export** — create `realm-export.json` with `fleet-api` confidential client (needed for `SCOPE_service` and admin client to work)
8. **Write fleet-api integration tests** — requires PostgreSQL/TimescaleDB via Testcontainers
