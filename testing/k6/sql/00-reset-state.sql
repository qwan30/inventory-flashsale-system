-- Clear benchmark-relevant mutable state in FK-safe order.
DELETE FROM inventory_reconciliation_drift;
DELETE FROM inventory_reconciliation_run;
DELETE FROM channel_inventory_snapshot;
DELETE FROM channel_sync_attempt;
DELETE FROM operation_idempotency;
DELETE FROM order_header;
DELETE FROM stock_reservation;
DELETE FROM flash_sale_campaign;
DELETE FROM outbox_event;
DELETE FROM inventory_item;
