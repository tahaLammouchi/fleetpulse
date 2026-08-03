-- ============================================================
-- V4__enable_timescaledb_extension.sql
-- TimescaleDB extension activation
-- ============================================================

-- TimescaleDB must be enabled before any hypertable creation or
-- time-series-specific function call (create_hypertable,
-- add_compression_policy, add_retention_policy, continuous aggregates).
-- The timescale/timescaledb Docker image ships the binary but does
-- NOT auto-enable the extension in each database at container start.
CREATE EXTENSION IF NOT EXISTS timescaledb;
