-- ============================================================
-- V3__create_notification_history.sql
-- Table: notification_history
-- ============================================================

-- ---------------------------------
-- TABLE: notification_history
-- ---------------------------------
CREATE TABLE notification_history (
    id         UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    alert_id   UUID        NOT NULL,
    sent_to    VARCHAR(255) NOT NULL,
    channel    VARCHAR(50)  NOT NULL,
    status     VARCHAR(50)  NOT NULL,
    sent_at    TIMESTAMP   NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_notification_channel CHECK (channel IN ('EMAIL', 'WEBHOOK')),
    CONSTRAINT chk_notification_status CHECK (status IN ('SENT', 'FAILED')),
    CONSTRAINT fk_notification_alert FOREIGN KEY (alert_id) REFERENCES alert (id) ON DELETE CASCADE
);

COMMENT ON TABLE notification_history IS 'Notification dispatch history linked to an alert';
COMMENT ON CONSTRAINT fk_notification_alert ON notification_history IS
    'CASCADE: notification history is compositionally tied to its alert;'
    ' deleting the alert removes its notification records';

CREATE INDEX idx_notification_alert
    ON notification_history(alert_id);

CREATE INDEX idx_notification_sent_at
    ON notification_history(sent_at DESC);

CREATE INDEX idx_notification_failed
    ON notification_history(created_at DESC)
    WHERE status='FAILED';
