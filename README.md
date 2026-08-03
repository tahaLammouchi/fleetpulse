
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
| Fleet API            | Spring Boot    | 8090         |
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

## Tests d'intégration — Fleet API

Les tests d'intégration (suffixe `*IT`) utilisent **Testcontainers** pour démarrer une instance PostgreSQL/TimescaleDB réelle dans un conteneur Docker.

### Prérequis

- Docker Desktop (ou équivalent) en cours d'exécution
- JDK 21

### Exécution

```bash
cd services/fleet-api

# Tests unitaires uniquement (surefire : *Test.java)
./mvnw test

# Tests unitaires + intégration (surefire + failsafe : *IT.java)
./mvnw verify
```

> **Note Windows** : Sur Windows, Testcontainers nécessite que Docker Desktop expose le nommé pipe correct.  
> Si vous rencontrez l'erreur `Could not find a valid Docker environment`, définissez la variable d'environnement :
> ```powershell
> $env:DOCKER_HOST="npipe:////./pipe/dockerDesktopLinuxEngine"
> ```
> Ou ajoutez les propriétés JVM `-Dtestcontainers.dockerclient.strategy=EnvironmentAndSystemPropertyClientProviderStrategy`.

### Architecture des tests

| Couche | Technologie | Classe de base |
|--------|-------------|----------------|
| Configuration conteneur | Testcontainers + JUnit Jupiter | `AbstractIntegrationTest` |
| Tests Repository | `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` | Hérite de `AbstractIntegrationTest` |
| Tests Controller | `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `WebTestClient` | Hérite de `AbstractIntegrationTest` |
| Authentification mockée | `@WithMockUser` / `@WithMockJwtAuth` | `@Import(TestSecurityBeans.class)` |

### Détail des tests

**Repository IT** (7 classes) :
- `FleetRepositoryIT` — CRUD, contrainte NOT NULL
- `VehicleRepositoryIT` — CRUD, unicité plaque, clé étrangère, pagination
- `InterventionRepositoryIT` — CRUD, associations, clôture, cascade SET NULL
- `AlertThresholdRepositoryIT` — CRUD, unicité composite, index global partiel
- `AlertRepositoryIT` — CRUD, changement de statut, clé étrangère
- `TelemetryReadingsIT` — Hypertable, insertion série temporelle, requête par plage
- `AnomalyScoresIT` — Hypertable, contrainte CHECK score, requête par modèle

**Controller IT** (5 classes) — Chacune teste les scénarios : nominal (2xx), validation (4xx), autorisation (403), non authentifié (401).

### Migration V10 (seed data)

Le fichier `V10__seed_sample_data.sql` a été déplacé de `db/migration/` vers `db/seed/` pour éviter son exécution dans les tests. Il reste chargé en environnement de développement et production via la configuration `flyway.locations` dans `application.yaml`.

### CI

Le pipeline GitHub Actions exécute :
1. `./mvnw checkstyle:check` — lint
2. `./mvnw compile` — compilation
3. `./mvnw test` — tests unitaires
4. `./mvnw verify` — tests intégration (Testcontainers utilise Docker natif du runner)

Aucun service container PostgreSQL dédié n'est nécessaire — Testcontainers gère le cycle de vie du conteneur et son nettoyage via Ryuk.

---

## License

This project is developed for educational purposes as part of an engineering software project.
