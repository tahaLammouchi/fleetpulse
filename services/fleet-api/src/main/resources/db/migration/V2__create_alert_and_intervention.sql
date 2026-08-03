-- ============================================================
-- V2__create_alert_and_intervention.sql
-- Tables: alert, alert_threshold, intervention
-- ============================================================

-- ---------------------------------
-- TABLE: alert
-- ---------------------------------
CREATE TABLE alert (
    id                   UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    vehicle_id           UUID        NOT NULL,
    anomaly_score_value  DOUBLE PRECISION NOT NULL,
    model_version        VARCHAR(255) NOT NULL,
    status               VARCHAR(50)  NOT NULL,
    triggered_at         TIMESTAMP   NOT NULL,
    acknowledged_at      TIMESTAMP,
    resolved_at          TIMESTAMP,
    acknowledged_by      UUID,
    created_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_alert_status CHECK (status IN ('NEW', 'ACKNOWLEDGED', 'RESOLVED')),
    CONSTRAINT fk_alert_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id) ON DELETE RESTRICT,
    CONSTRAINT fk_alert_acknowledged_by FOREIGN KEY (acknowledged_by) REFERENCES app_user (id) ON DELETE SET NULL
);

COMMENT ON TABLE alert IS 'Anomaly alert triggered for a vehicle';
COMMENT ON CONSTRAINT fk_alert_vehicle ON alert IS
    'RESTRICT: vehicle with alerts cannot be deleted (audit trail preservation)';
COMMENT ON CONSTRAINT fk_alert_acknowledged_by ON alert IS
    'SET NULL: if the acknowledging user is deleted, the alert remains but loses the user reference';

CREATE INDEX idx_alert_vehicle_status ON alert (vehicle_id, status);
CREATE INDEX idx_alert_triggered_at ON alert (triggered_at);

-- ---------------------------------
-- TABLE: alert_threshold
-- ---------------------------------
-- Note: AlertThreshold intentionally has NO foreign key to vehicle or
-- anomaly_scores. The entity defines no JPA relationship to those tables;
-- the threshold is a pure configuration value keyed by vehicle type and
-- model version only.
CREATE TABLE alert_threshold (
    id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    vehicle_type    VARCHAR(50),
    model_version   VARCHAR(255) NOT NULL,
    threshold_value DOUBLE PRECISION NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_threshold_type_version UNIQUE (vehicle_type, model_version),
    CONSTRAINT chk_alertthreshold_vehicle_type CHECK (vehicle_type IN ('VAN', 'TRUCK', 'CAR'))
);

COMMENT ON TABLE alert_threshold IS
    'Anomaly score thresholds per vehicle type and model version';
COMMENT ON CONSTRAINT uq_threshold_type_version ON alert_threshold IS
    'Standard UNIQUE constraint; PostgreSQL treats NULL != NULL in unique constraints,'
    ' so multiple rows with (NULL, ''v1'') would be allowed. The partial index below'
    ' closes this gap for global (vehicle_type IS NULL) thresholds.';

-- Partial unique index: guarantees at most one global (vehicle_type IS NULL)
-- threshold per model_version, compensating for the NULL-inequality behaviour
-- of standard PostgreSQL UNIQUE constraints.
CREATE UNIQUE INDEX uq_threshold_global_version
    ON alert_threshold (model_version)
    WHERE vehicle_type IS NULL;

-- ---------------------------------
-- TABLE: intervention
-- ---------------------------------
CREATE TABLE intervention (
    id             UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    vehicle_id     UUID        NOT NULL,
    technician_id  UUID,
    alert_id       UUID,
    status         VARCHAR(50) NOT NULL,
    description    VARCHAR(255),
    opened_at      TIMESTAMP   NOT NULL,
    closed_at      TIMESTAMP,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_intervention_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'CLOSED')),
    CONSTRAINT fk_intervention_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id) ON DELETE RESTRICT,
    CONSTRAINT fk_intervention_technician FOREIGN KEY (technician_id) REFERENCES app_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_intervention_alert FOREIGN KEY (alert_id) REFERENCES alert (id) ON DELETE RESTRICT
);

COMMENT ON TABLE intervention IS 'Maintenance intervention linked to a vehicle alert';
COMMENT ON CONSTRAINT fk_intervention_vehicle ON intervention IS
    'RESTRICT: vehicle with interventions cannot be deleted (audit trail)';
COMMENT ON CONSTRAINT fk_intervention_technician ON intervention IS
    'SET NULL: if technician user is deleted, intervention history is preserved';
COMMENT ON CONSTRAINT fk_intervention_alert ON intervention IS
    'RESTRICT: an Alert referenced by an Intervention cannot be deleted (audit trail preservation)';

CREATE INDEX idx_intervention_vehicle ON intervention (vehicle_id);
CREATE INDEX idx_intervention_technician ON intervention (technician_id);
CREATE INDEX idx_intervention_alert ON intervention (alert_id);
CREATE INDEX idx_intervention_active
    ON intervention(opened_at DESC)
    WHERE status IN ('OPEN', 'IN_PROGRESS');