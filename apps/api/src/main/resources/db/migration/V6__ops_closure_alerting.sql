ALTER TABLE inventory_reconciliation_run
    ADD COLUMN trigger_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL' AFTER id,
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED' AFTER open_drift_count,
    ADD COLUMN failure_message VARCHAR(512) NULL AFTER status;

CREATE INDEX idx_inventory_reconciliation_run_trigger_status_created_at
    ON inventory_reconciliation_run (trigger_type, status, created_at);

CREATE INDEX idx_channel_inventory_snapshot_synced_at
    ON channel_inventory_snapshot (synced_at);
