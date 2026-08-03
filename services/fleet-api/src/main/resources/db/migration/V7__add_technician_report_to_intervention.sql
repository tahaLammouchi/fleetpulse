-- ============================================================
-- V7__add_technician_report_to_intervention.sql
-- Ajout du champ technician_report à la table intervention
-- ============================================================

-- Business need: 'description' is filled by the FLEET_MANAGER when the
-- intervention is opened (the reported problem / reason for the
-- intervention). A separate field is needed for the TECHNICIAN's own
-- account filled at closure time (diagnosis + action taken), e.g.
-- "The issue was a worn brake pad. I replaced it and tested the vehicle."
-- Keeping these as two distinct columns preserves a clean audit trail
-- of who said what, and when, rather than overwriting the manager's
-- original description with the technician's closing notes.

ALTER TABLE intervention
    ADD COLUMN technician_report VARCHAR(1000);

COMMENT ON COLUMN intervention.description IS
    'Problem description provided by the FLEET_MANAGER when the intervention is opened';

COMMENT ON COLUMN intervention.technician_report IS
    'Diagnosis and action report written by the TECHNICIAN at closure time (UC-031)';