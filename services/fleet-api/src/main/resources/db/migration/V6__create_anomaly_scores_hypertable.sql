-- ============================================================
-- V6__create_anomaly_scores_hypertable.sql
-- Hypertable: anomaly_scores
-- ============================================================

-- 1. Table definition with composite primary key (time, vehicle_id, model_version).
--    The 3-column PK allows two different model versions to produce a score for
--    the same vehicle at the same timestamp without key collision (A/B test).
CREATE TABLE anomaly_scores (
    time           TIMESTAMPTZ   NOT NULL,
    vehicle_id     UUID          NOT NULL,
    score          DOUBLE PRECISION NOT NULL,
    model_version  VARCHAR(50)   NOT NULL,
    PRIMARY KEY (time, vehicle_id, model_version)
);

-- 2. CHECK constraint: score must be in [0, 1], matching the
--    minimum/maximum defined in contracts/anomaly-score.schema.json.
ALTER TABLE anomaly_scores
    ADD CONSTRAINT chk_anomaly_scores_score
    CHECK (score >= 0 AND score <= 1);

-- 3. Convert to hypertable partitioned by time with 1-day chunks.
--    Must be called BEFORE any compression or retention policy.
SELECT create_hypertable(
    'anomaly_scores',
    'time',
    chunk_time_interval => INTERVAL '1 day'
);

-- 4. Index for the "vehicle anomaly history" query pattern:
--    fetch all scores for a vehicle ordered by time descending.
CREATE INDEX idx_anomaly_scores_vehicle_time
    ON anomaly_scores (vehicle_id, time DESC);

-- 5. Index for the alerting service: find the latest score for a
--    specific vehicle + model_version pair (used to compare against
--    alert_threshold thresholds).
CREATE INDEX idx_anomaly_scores_vehicle_model_time
    ON anomaly_scores (vehicle_id, model_version, time DESC);

-- 6. Compression policy: automatically compress chunks older than 7 days.
--    Segment by vehicle_id for optimal row-group locality within compressed
--    chunks (same rationale as telemetry_readings).
ALTER TABLE anomaly_scores SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'vehicle_id'
);
SELECT add_compression_policy('anomaly_scores', INTERVAL '7 days');

-- 7. Retention policy: automatically drop data older than 90 days.
--    Aligned with telemetry_readings retention so both time-series
--    remain temporally coherent for cross-series analysis.
SELECT add_retention_policy('anomaly_scores', INTERVAL '90 days');

-- 8. No foreign key to vehicle(id) nor alert_threshold.
--    Same rationale as telemetry_readings: high write throughput from
--    ML inference service via RabbitMQ. A FK would add a per-INSERT
--    lookup penalty. Referential integrity is handled by the
--    application layer. No FK to alert_threshold either: the score
--    is compared against threshold values at query time, not at
--    insert time — consistent with the resolution-by-value design
--    already applied in V2 for alert_threshold (no FK there either).
