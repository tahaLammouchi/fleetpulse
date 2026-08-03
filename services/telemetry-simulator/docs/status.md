# Telemetry Simulator — Project Status

> Last updated: 2026-07-27

## Overview

The Telemetry Simulator generates synthetic telemetry data to feed the FleetPulse pipeline. It is the first stage in the pipeline (port 8005) and has no upstream dependency.

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
| pydantic-settings | 2.3.0 | Active |
| pytest | 8.2.0 | Listed (no tests written) |
| httpx | 0.27.0 | Listed (no tests written) |
| requests | 2.32.0 | Listed (not yet used) |

**Note:** No RabbitMQ library (`pika`) is listed in `requirements.txt` — this service has no messaging dependencies yet.

---

## Application Structure

| File | Status | Notes |
|------|--------|-------|
| `app/main.py` | ✅ Present | FastAPI app, includes health router, `GET /` root endpoint |
| `app/health.py` | ✅ Present | `/healthz` and `/readyz` endpoints |
| `app/config.py` | ✅ Present | Pydantic Settings with RabbitMQ defaults |
| `app/schemas.py` | ❌ Empty | No Pydantic models or schema loading |
| `app/consumer.py` | ❌ Missing | Does not exist (pipeline source — expected) |
| `app/publisher.py` | ❌ Missing | Does not exist (should publish telemetry) |
| `app/__init__.py` | ✅ Present | Package marker |
| `Dockerfile` | ❌ Empty | No container build instructions |
| `requirements.txt` | ✅ Present | 6 dependencies (no pika) |
| `.env.example` | ✅ Present | Template for env vars |
| `README.md` | ✅ Present | Basic run/test instructions |
| `tests/` | ❌ Empty | No test files |

---

## What's Done

- FastAPI application boots and serves `/healthz`, `/readyz`, and `/` endpoints
- Pydantic Settings model reads from `.env` (RabbitMQ host/port, service name)
- CI workflow configured (`telemetry-simulator-ci.yml`) with ruff linting and pytest

## What's Not Done

- **RabbitMQ wiring** — `publisher.py` does not exist; `consumer.py` does not exist (expected for pipeline source)
- **Tests** — `tests/` directory is empty
- **Dockerfile** — empty, no container build steps
- **`/readyz`** — returns `{"status": "ready"}` unconditionally; RabbitMQ check not implemented
- **Schemas** — `schemas.py` is empty
- **Telemetry generation** — no synthetic data generation logic
- **RabbitMQ dependency** — `pika` is not in `requirements.txt`

---

## Known Issues

1. **Copy-paste title bug** — `main.py` uses `title="Ingestion Service"` instead of `title="Telemetry Simulator"`.
2. **`/readyz` is a stub** — has a TODO comment (`# à compléter : vérifier connexion RabbitMQ`) but no actual check.
3. **No publisher** — `publisher.py` does not exist (pipeline source should publish telemetry).
4. **Dockerfile is empty** — no container build instructions.
5. **`schemas.py` is empty** — no Pydantic models or schema loading.
6. **No RabbitMQ library** — `pika` is not in `requirements.txt`.
