-- ============================================================
-- V5__create_telemetry_hypertable.sql
-- Hypertable: telemetry_readings
-- ============================================================

-- 1. Table definition with composite primary key (time, vehicle_id).
--    TimescaleDB requires the partitioning column (time) to be part
--    of any primary key or unique constraint.
CREATE TABLE telemetry_readings (
    time        TIMESTAMPTZ NOT NULL,
    vehicle_id  UUID        NOT NULL,
    temperature DOUBLE PRECISION,
    vibration   DOUBLE PRECISION,
    oil_level   DOUBLE PRECISION,
    rpm         DOUBLE PRECISION,
    mileage     DOUBLE PRECISION,
    PRIMARY KEY (time, vehicle_id)
);

-- 2. Convert to hypertable partitioned by time with 1-day chunks.
--    Must be called BEFORE any compression or retention policy.
SELECT create_hypertable(
    'telemetry_readings',
    'time',
    chunk_time_interval => INTERVAL '1 day'
);

-- 3. Index for the "vehicle telemetry history" query pattern
--    (GET /api/vehicles/{id}/telemetry): fetch all readings for a
--    vehicle ordered by time descending.
CREATE INDEX idx_telemetry_readings_vehicle_time
    ON telemetry_readings (vehicle_id, time DESC);

-- 4. Compression policy: automatically compress chunks older than 7 days.
--    Segment by vehicle_id for optimal row-group locality within compressed
--    chunks (all readings for the same vehicle are co-located).
ALTER TABLE telemetry_readings SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'vehicle_id'
);
SELECT add_compression_policy('telemetry_readings', INTERVAL '7 days');

-- 5. Retention policy: automatically drop data older than 90 days.
--    Keeps the hypertable bounded and aligns with anomaly_scores retention
--    for cross-series analysis coherence.
SELECT add_retention_policy('telemetry_readings', INTERVAL '90 days');

-- 6. Continuous aggregate: hourly rollup of temperature and vibration
--    per vehicle. Enables the dashboard to read pre-aggregated data
--    without scanning the full raw hypertable on every refresh.
CREATE MATERIALIZED VIEW telemetry_hourly
    WITH (timescaledb.continuous) AS
    SELECT
        time_bucket('1 hour', time) AS bucket,
        vehicle_id,
        AVG(temperature) AS avg_temperature,
        AVG(vibration)   AS avg_vibration
    FROM telemetry_readings
    GROUP BY bucket, vehicle_id
    WITH NO DATA;

-- Refresh policy: refresh the continuous aggregate every hour.
SELECT add_continuous_aggregate_policy('telemetry_hourly',
    start_offset    => INTERVAL '3 hours',
    end_offset      => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour'
);

-- 7. No foreign key to vehicle(id).
--    Telemetry data is high-volume (millions of rows/day) and the
--    write path is a RabbitMQ consumer. A FK would force a lookup
--    on every INSERT, severely degrading throughput. Referential
--    integrity is enforced at the application level by the ingestion
--    service, which resolves vehicle_id before writing.
