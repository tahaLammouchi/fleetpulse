-- ============================================================
-- V1__create_enums_and_core_tables.sql
-- Core tables: fleet, app_user, vehicle
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------
-- TABLE: fleet
-- ---------------------------------
CREATE TABLE fleet (
    id          UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE fleet IS 'Fleet grouping for vehicles';

-- ---------------------------------
-- TABLE: app_user
-- ---------------------------------
-- Note: @Index(name = "idx_appuser_keycloak_id", unique = true) on the entity
-- is redundant with @Column(unique = true) on keycloakId. The unique constraint
-- below already creates a unique B-tree index in PostgreSQL.
CREATE TABLE app_user (
    id           UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    keycloak_id  VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    full_name    VARCHAR(255) NOT NULL,
    role         VARCHAR(50)  NOT NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_app_user_keycloak_id UNIQUE (keycloak_id),
    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT chk_app_user_role CHECK (role IN ('FLEET_MANAGER', 'TECHNICIAN', 'ADMIN'))
);

COMMENT ON TABLE app_user IS 'Application user synced from Keycloak';

-- ---------------------------------
-- TABLE: vehicle
-- ---------------------------------
CREATE TABLE vehicle (
    id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    fleet_id        UUID        NOT NULL,
    license_plate   VARCHAR(20) NOT NULL,
    brand           VARCHAR(255),
    model           VARCHAR(255),
    vehicle_type    VARCHAR(50) NOT NULL,
    status          VARCHAR(50) NOT NULL,
    registered_at   TIMESTAMP   NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_vehicle_license_plate UNIQUE (license_plate),
    CONSTRAINT chk_vehicle_vehicle_type CHECK (vehicle_type IN ('VAN', 'TRUCK', 'CAR')),
    CONSTRAINT chk_vehicle_status CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'DECOMMISSIONED')),
    CONSTRAINT fk_vehicle_fleet FOREIGN KEY (fleet_id) REFERENCES fleet (id) ON DELETE RESTRICT
);

COMMENT ON TABLE vehicle IS 'Fleet vehicle';
COMMENT ON CONSTRAINT fk_vehicle_fleet ON vehicle IS
    'Aggregation: a fleet cannot be deleted while it has vehicles (RESTRICT preserves referential integrity)';

CREATE INDEX idx_vehicle_fleet ON vehicle (fleet_id);
CREATE INDEX idx_vehicle_type_status ON vehicle (vehicle_type, status);
