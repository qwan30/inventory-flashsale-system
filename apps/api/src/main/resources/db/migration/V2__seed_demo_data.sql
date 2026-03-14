INSERT INTO inventory_item (sku, available_qty, reserved_qty, sold_qty, version)
VALUES ('SKU-DEMO-001', 100, 0, 0, 0)
ON DUPLICATE KEY UPDATE sku = sku;

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
)
ON DUPLICATE KEY UPDATE id = id;
