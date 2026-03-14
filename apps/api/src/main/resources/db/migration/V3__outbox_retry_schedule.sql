ALTER TABLE outbox_event
    ADD COLUMN next_attempt_at TIMESTAMP(6) NULL AFTER last_error;

UPDATE outbox_event
SET next_attempt_at = updated_at
WHERE status = 'FAILED'
  AND next_attempt_at IS NULL;

CREATE INDEX idx_outbox_status_next_attempt_at_created_at
    ON outbox_event (status, next_attempt_at, created_at);
