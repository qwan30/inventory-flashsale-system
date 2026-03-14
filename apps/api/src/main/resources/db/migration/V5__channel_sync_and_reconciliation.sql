CREATE TABLE channel_sync_attempt (
    id VARCHAR(64) PRIMARY KEY,
    outbox_event_id VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    sku VARCHAR(64) NULL,
    event_type VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    available_qty INT NULL,
    reserved_qty INT NULL,
    sold_qty INT NULL,
    status VARCHAR(32) NOT NULL,
    failure_type VARCHAR(32) NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(512) NULL,
    next_attempt_at TIMESTAMP(6) NULL,
    synced_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_channel_sync_outbox_channel UNIQUE (outbox_event_id, channel),
    CONSTRAINT fk_channel_sync_outbox_event FOREIGN KEY (outbox_event_id) REFERENCES outbox_event (id)
);

CREATE INDEX idx_channel_sync_status_created_at
    ON channel_sync_attempt (status, created_at);

CREATE INDEX idx_channel_sync_status_failure_next_attempt
    ON channel_sync_attempt (status, failure_type, next_attempt_at, created_at);

CREATE TABLE channel_inventory_snapshot (
    id VARCHAR(96) PRIMARY KEY,
    channel VARCHAR(32) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    available_qty INT NOT NULL,
    reserved_qty INT NOT NULL,
    sold_qty INT NOT NULL,
    source_outbox_event_id VARCHAR(64) NOT NULL,
    synced_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_channel_inventory_snapshot UNIQUE (channel, sku),
    CONSTRAINT fk_channel_inventory_snapshot_outbox_event FOREIGN KEY (source_outbox_event_id) REFERENCES outbox_event (id)
);

CREATE TABLE inventory_reconciliation_run (
    id VARCHAR(64) PRIMARY KEY,
    scanned_sku_count INT NOT NULL DEFAULT 0,
    scanned_snapshot_count INT NOT NULL DEFAULT 0,
    open_drift_count INT NOT NULL DEFAULT 0,
    completed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE inventory_reconciliation_drift (
    id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    central_available_qty INT NOT NULL,
    central_reserved_qty INT NOT NULL,
    central_sold_qty INT NOT NULL,
    observed_available_qty INT NOT NULL,
    observed_reserved_qty INT NOT NULL,
    observed_sold_qty INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    resolution_note VARCHAR(512) NULL,
    resolved_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_inventory_reconciliation_drift_run FOREIGN KEY (run_id) REFERENCES inventory_reconciliation_run (id)
);

CREATE INDEX idx_inventory_reconciliation_drift_status_created_at
    ON inventory_reconciliation_drift (status, created_at);
