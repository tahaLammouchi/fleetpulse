# Ingestion Service — Project Status

> Last updated: 2026-07-27

## Overview

The Ingestion Service receives and validates telemetry data from the Telemetry Simulator. It is the second stage in the FleetPulse pipeline (port 8001).

```
telemetry-simulator → ingestion → ml-inference → alerting → notification
```

---

## Tech Stack

| Component | Version | Status |
|---|---|---|
| Python | 3.12 | Ready |
| FastAPI | 0.111.0 | Ready |
| uvicorn | 0.30.0 | Ready |
| pika | 1.3.2 | Listed (not yet wired) |
| pydantic-settings | 2.3.0 | Active |
| jsonschema | 4.22.0 | Active |
| pytest | 8.2.0 | Listed (no tests written) |
| httpx | 0.27.0 | Listed (no tests written) |

---

## Application Structure

| File | Status | Notes |
|------|--------|-------|
| `app/main.py` | ✅ Present | FastAPI app, includes health router, `GET /` root endpoint |
| `app/health.py` | ✅ Present | `/healthz` and `/readyz` endpoints |
| `app/config.py` | ✅ Present | Pydantic Settings with RabbitMQ defaults |
| `app/schemas.py` | ✅ Present | Loads `telemetry.schema.json` from shared contracts |
| `app/consumer.py` | ❌ Empty | No RabbitMQ consumer logic |
| `app/publisher.py` | ❌ Empty | No RabbitMQ publisher logic |
| `app/__init__.py` | ✅ Present | Package marker |
| `Dockerfile` | ❌ Empty | No container build instructions |
| `requirements.txt` | ✅ Present | 7 dependencies |
| `.env.example` | ✅ Present | Template for env vars |
| `README.md` | ✅ Present | Basic run/test instructions |
| `tests/` | ❌ Empty | No test files |

---

## What's Done

- FastAPI application boots and serves `/healthz`, `/readyz`, and `/` endpoints
- Pydantic Settings model reads from `.env` (RabbitMQ host/port, service name)
- JSON Schema loader reads `telemetry.schema.json` from shared `contracts/`

## What's Not Done

- **RabbitMQ wiring** — `consumer.py` and `publisher.py` are empty stubs
- **Tests** — `tests/` directory is empty
- **Dockerfile** — empty, no container build steps
- **`/readyz`** — returns `{"status": "ready"}` unconditionally; RabbitMQ check not implemented
- **CI workflow** — `ingestion-ci.yml` is empty (0 bytes)
- **Business logic** — no telemetry validation or forwarding code

---

## Known Issues

1. **Copy-paste title bug** — `main.py` uses `title="Ingestion Service"` (correct by coincidence, but likely copy-pasted).
2. **`/readyz` is a stub** — has a TODO comment (`# à compléter : vérifier connexion RabbitMQ`) but no actual check.
3. **No RabbitMQ wiring** — both consumer and publisher are empty stubs.
4. **Dockerfile is empty** — no container build instructions.
5. **CI workflow is empty** — `ingestion-ci.yml` has 0 bytes.
