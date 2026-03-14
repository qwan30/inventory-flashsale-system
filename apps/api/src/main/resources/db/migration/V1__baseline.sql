CREATE TABLE inventory_item (
    sku VARCHAR(64) PRIMARY KEY,
    available_qty INT NOT NULL,
    reserved_qty INT NOT NULL,
    sold_qty INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE flash_sale_campaign (
    id VARCHAR(64) PRIMARY KEY,
    sku VARCHAR(64) NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NOT NULL,
    quota INT NOT NULL,
    reserved_quota INT NOT NULL DEFAULT 0,
    sold_quota INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_flash_sale_campaign_inventory FOREIGN KEY (sku) REFERENCES inventory_item (sku)
);

CREATE TABLE stock_reservation (
    id VARCHAR(64) PRIMARY KEY,
    sku VARCHAR(64) NOT NULL,
    campaign_id VARCHAR(64),
    channel VARCHAR(32) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    confirm_idempotency_key VARCHAR(128),
    order_id VARCHAR(64),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_stock_reservation_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_stock_reservation_inventory FOREIGN KEY (sku) REFERENCES inventory_item (sku),
    CONSTRAINT fk_stock_reservation_campaign FOREIGN KEY (campaign_id) REFERENCES flash_sale_campaign (id)
);

CREATE TABLE order_header (
    id VARCHAR(64) PRIMARY KEY,
    reservation_id VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_order_header_reservation UNIQUE (reservation_id),
    CONSTRAINT fk_order_header_reservation FOREIGN KEY (reservation_id) REFERENCES stock_reservation (id)
);

CREATE TABLE outbox_event (
    id VARCHAR(64) PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    published_at TIMESTAMP(6) NULL,
    last_error VARCHAR(512),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_stock_reservation_status_expires_at ON stock_reservation (status, expires_at);
CREATE INDEX idx_outbox_status_created_at ON outbox_event (status, created_at);

