# Notification Service — Project Status

> Last updated: 2026-07-27

## Overview

The Notification Service sends notifications (email/webhook) based on alert events received from the Alerting Service. It is the final stage in the FleetPulse pipeline (port 8004).

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
| jsonschema | 4.22.0 | Listed (not yet used) |
| pytest | 8.2.0 | Listed (no tests written) |
| httpx | 0.27.0 | Listed (no tests written) |
| requests | 2.32.0 | Listed (not yet used) |

---

## Application Structure

| File | Status | Notes |
|------|--------|-------|
| `app/main.py` | ✅ Present | FastAPI app, includes health router, `GET /` root endpoint |
| `app/health.py` | ✅ Present | `/healthz` and `/readyz` endpoints |
| `app/config.py` | ✅ Present | Pydantic Settings with RabbitMQ defaults |
| `app/schemas.py` | ❌ Empty | No Pydantic models or schema loading |
| `app/consumer.py` | ❌ Empty | No RabbitMQ consumer logic |
| `app/publisher.py` | ❌ Missing | Does not exist (pipeline sink — may be intentional) |
| `app/__init__.py` | ✅ Present | Package marker |
| `Dockerfile` | ❌ Empty | No container build instructions |
| `requirements.txt` | ✅ Present | 8 dependencies |
| `.env.example` | ✅ Present | Template for env vars |
| `README.md` | ✅ Present | Basic run/test instructions |
| `tests/` | ❌ Empty | No test files |

---

## What's Done

- FastAPI application boots and serves `/healthz`, `/readyz`, and `/` endpoints
- Pydantic Settings model reads from `.env` (RabbitMQ host/port, service name)
- CI workflow configured (`notification-ci.yml`) with ruff linting and pytest

## What's Not Done

- **RabbitMQ wiring** — `consumer.py` is an empty stub; `publisher.py` does not exist
- **Tests** — `tests/` directory is empty
- **Dockerfile** — empty, no container build steps
- **`/readyz`** — returns `{"status": "ready"}` unconditionally; RabbitMQ check not implemented
- **Schemas** — `schemas.py` is empty (no Pydantic models or JSON Schema loading)
- **Business logic** — no notification sending code (email, webhook)

---

## Known Issues

1. **Copy-paste title bug** — `main.py` uses `title="Ingestion Service"` instead of `title="Notification Service"`.
2. **`/readyz` is a stub** — has a TODO comment (`# à compléter : vérifier connexion RabbitMQ`) but no actual check.
3. **No RabbitMQ wiring** — `consumer.py` is empty; `publisher.py` is missing entirely.
4. **Dockerfile is empty** — no container build instructions.
5. **`schemas.py` is empty** — no Pydantic models or JSON Schema loading implemented.
