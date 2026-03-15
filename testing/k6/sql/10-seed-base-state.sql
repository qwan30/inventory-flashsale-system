-- Deterministic benchmark seed contract.
INSERT INTO inventory_item (sku, available_qty, reserved_qty, sold_qty, version)
VALUES ('SKU-DEMO-001', 100, 0, 0, 0);

INSERT INTO flash_sale_campaign (
    id,
    sku,
    starts_at,
    ends_at,
    quota,
    reserved_quota,
    sold_quota,
    status
)
VALUES (
    'campaign-demo-001',
    'SKU-DEMO-001',
    '2026-01-01 00:00:00',
    '2027-01-01 00:00:00',
    50,
    0,
    0,
    'ACTIVE'
);

INSERT INTO flash_sale_campaign (
    id,
    sku,
    starts_at,
    ends_at,
    quota,
    reserved_quota,
    sold_quota,
    status
)
VALUES (
    'campaign-ended-001',
    'SKU-DEMO-001',
    '2024-01-01 00:00:00',
    '2024-12-31 23:59:59',
    50,
    0,
    0,
    'ENDED'
);
