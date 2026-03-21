ALTER TABLE outbox_event
    ADD COLUMN event_version INT NOT NULL DEFAULT 1 AFTER event_type;
