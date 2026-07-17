
# FleetPulse

FleetPulse is a microservices-based fleet management and predictive maintenance platform. It collects vehicle telemetry, analyzes incoming data using machine learning, detects anomalies, and generates alerts and notifications in near real time.

This repository contains the initial project skeleton for all backend services and the shared infrastructure required for development.

---

## Project Architecture

```text
fleetpulse/
│
├── services/
│   ├── fleet-api/                # Spring Boot API Gateway
│   ├── ingestion-service/        # FastAPI - Telemetry ingestion
│   ├── ml-inference-service/     # FastAPI - ML inference
│   ├── alerting-service/         # FastAPI - Alert generation
│   ├── notification-service/     # FastAPI - Notification delivery
│   └── simulator-service/        # FastAPI - Vehicle telemetry simulator
│
├── contracts/                    # Shared JSON event schemas
│
├── infrastructure/
│   ├── docker/
│   ├── postgres/
│   ├── rabbitmq/
│   └── monitoring/
│
├── docs/
├── scripts/
├── docker-compose.yml
└── README.md
```

---

## Technology Stack

### Backend

* Java 21
* Spring Boot 4
* Spring Web
* Spring Data JPA
* Spring Security
* Python 
* FastAPI

### Infrastructure

* PostgreSQL / TimescaleDB
* RabbitMQ
* Docker
* Kubernetes

### Future Components

* Keycloak
* Prometheus
* Grafana
* GitHub Actions CI/CD

---

## Services

| Service              | Technology     | Default Port |
| -------------------- | -------------- | ------------ |
| Fleet API            | Spring Boot    | 8080         |
| Ingestion Service    | FastAPI        | 8001         |
| ML Inference Service | FastAPI        | 8002         |
| Alerting Service     | FastAPI        | 8003         |
| Notification Service | FastAPI        | 8004         |
| Simulator Service    | FastAPI        | 8005         |
| PostgreSQL           | PostgreSQL     | 5432         |
| RabbitMQ             | RabbitMQ       | 5672         |
| RabbitMQ Management  | Web UI         | 15672        |
| Keycloak             | Authentication | 8081         |

---

## Current Status

### Sprint 1 – Project Initialization

* Project repository created
* Monorepo architecture established
* Spring Boot Fleet API initialized
* FastAPI microservice skeletons created
* Shared contracts directory prepared
* Infrastructure directories created
* Initial documentation structure added

---

## Planned Communication Flow

```text
Simulator
    │
    ▼
Ingestion Service
    │
    ▼
RabbitMQ
    │
    ▼
ML Inference
    │
    ▼
Alerting
    │
    ▼
Notification
```

---

## Getting Started

Clone the repository:

```bash
git clone <repository-url>
cd fleetpulse
```

The implementation of business logic, messaging, database integration, and authentication will be developed incrementally during the next project milestones.

---

## License

This project is developed for educational purposes as part of an engineering software project.
