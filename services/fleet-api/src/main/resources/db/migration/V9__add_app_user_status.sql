-- =====================================================
-- Add status column to app_user table
-- =====================================================

ALTER TABLE app_user
    ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ENABLED';


-- Constraint to validate allowed user statuses
ALTER TABLE app_user
    ADD CONSTRAINT chk_app_user_status
        CHECK (status IN ('ENABLED', 'DISABLED'));