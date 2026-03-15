CREATE TABLE alert_delivery_state (
    alert_code VARCHAR(64) PRIMARY KEY,
    last_observed_status VARCHAR(32) NOT NULL,
    last_observed_at TIMESTAMP(6) NOT NULL,
    last_notified_status VARCHAR(32) NULL,
    last_sent_at TIMESTAMP(6) NULL,
    last_error VARCHAR(512) NULL,
    consecutive_failures INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_alert_delivery_state_observed_status
    ON alert_delivery_state (last_observed_status, updated_at);
