# FleetPulse — AGENTS.md

Quick-reference for agents working in this monorepo. Prioritise executable sources over prose.

## Architecture

6 microservices under `services/`, piped through RabbitMQ:

```
telemetry-simulator (:8005) → ingestion (:8001) → ml-inference (:8002) → alerting (:8003) → notification (:8004)
                                                                                              ↘ fleet-api (:8090, Java)
```

- **fleet-api** — Java 21 / Spring Boot 4 / Maven (`./mvnw`). JPA + Flyway + OAuth2 (Keycloak JWT).
- **5 Python services** — Python 3.12 / FastAPI / pip (`uvicorn app.main:app`). Each has its own `requirements.txt` and `Dockerfile`.

Shared JSON Schemas (RabbitMQ contract): `contracts/` — `telemetry.schema.json`, `anomaly-score.schema.json`, `alert-event.schema.json`.

## Infrastructure

```
docker-compose up -d
```

Starts PostgreSQL+TimescaleDB (pg16), RabbitMQ 3.13 (AMQP :5672, UI :15672), Keycloak 25.0 (:8081).
All default credentials are `fleetpulse`/`fleetpulse` (dev only, from `.env`).

**Note:** `README.md` references an `infrastructure/` directory that does not exist on disk — the `docker-compose.yml` at the repo root is the single source of truth for infra.

## Developing a Python service

```bash
cd services/<name>
pip install -r requirements.txt
uvicorn app.main:app --reload --port <port>
```

Ports: 8001 ingestion, 8002 ml-inference, 8003 alerting, 8004 notification, 8005 telemetry-simulator.

Each Python service exposes `/healthz` and `/readyz` endpoints via `app/health.py`.

CI runs: `ruff check .` then `pytest tests/ -v`. Python version: 3.12, ruff uses defaults (no config file).

## Developing the Java service

```bash
cd services/fleet-api
./mvnw compile          # build
./mvnw checkstyle:check # lint
./mvnw test             # unit + integration (needs Postgres, see CI for service config)
```

CI spins up a TimescaleDB service container and passes `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fleetpulse_test`. Uses Temurin JDK 21.

## Known quirks (verified)

- **README says fleet-api port 8080, but `application.yaml` sets `server.port: 8090`** — trust the yaml.
- **All 5 Python services have `title="Ingestion Service"` in `main.py`** — copy-paste bug; each should use its own name.
- **Flyway enabled with `ddl-auto: validate` but no migration scripts exist yet** — `db/migration/` is empty.
- **Security config stubs are empty** — `SecurityConfig.java` and `CurrentUserResolver.java` are placeholder classes with no beans/methods.
- **No repositories, services, or controllers implemented** — `repository/`, `service/`, `web/controller/`, `web/dto/`, `web/mapper/` directories exist but are empty. Only JPA entities and enums exist in `domain/`.
- **Existing `consumer.py`/`publisher.py` files are empty** — RabbitMQ wiring not yet implemented. Note: telemetry-simulator (pipeline source) has neither file; notification (pipeline sink) has only `consumer.py`.
- **`tests/` directories exist for all Python services but contain no test files** — first test written will be the first.
- **`keycloak/realm-export.json/` is an empty directory** — volume mount references it but no realm export exists yet.
- **`ingestion-ci.yml` is empty** (0 bytes) — the only CI file without content.
- **CI workflow files use inconsistent indentation** — `alerting-ci.yml` and `fleet-api-ci.yml` use spaces; `ml-inference-ci.yml` and `telemetry-simulator-ci.yml` use tabs.
- **`.gitignore` has markdown code-fence backticks** — first line is `` ```gitignore `` and last line is `` ``` ``, which is syntactically harmless but unintended.
- **No `pyproject.toml`, `ruff.toml`, `Makefile`, or `.pre-commit-config.yaml`** exists at any level.
