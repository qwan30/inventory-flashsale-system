CREATE TABLE operation_idempotency (
    id VARCHAR(64) PRIMARY KEY,
    operation_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    operation_value VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    response_payload TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_operation_idempotency_key UNIQUE (operation_type, resource_id, idempotency_key),
    CONSTRAINT uq_operation_idempotency_value UNIQUE (operation_type, resource_id, operation_value)
);
