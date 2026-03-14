# Data Model

**Last Updated:** 2026-03-15

## Current Tables

### `inventory_item`

Purpose:

- central inventory state per SKU

Important fields:

- `sku` primary key
- `available_qty`
- `reserved_qty`
- `sold_qty`
- `version`
- audit timestamps

Notes:

- `version` supports optimistic locking

### `flash_sale_campaign`

Purpose:

- flash sale window and quota definition for a SKU

Important fields:

- `id` primary key
- `sku` foreign key to `inventory_item`
- `starts_at`
- `ends_at`
- `quota`
- `reserved_quota`
- `sold_quota`
- `status`

### `stock_reservation`

Purpose:

- active and historical reservation records

Important fields:

- `id` primary key
- `sku` foreign key to `inventory_item`
- `campaign_id` foreign key to `flash_sale_campaign`
- `channel`
- `quantity`
- `status`
- `expires_at`
- `idempotency_key`
- `confirm_idempotency_key`
- `order_id`

Important constraints:

- unique `idempotency_key`
- index on `(status, expires_at)` for expiry scanning

### `order_header`

Purpose:

- order lifecycle state linked to a reservation

Important fields:

- `id` primary key
- `reservation_id` unique foreign key to `stock_reservation`
- `channel`
- `status`

### `outbox_event`

Purpose:

- durable event publish queue for downstream integrations

Important fields:

- `id` primary key
- `aggregate_type`
- `aggregate_id`
- `event_type`
- `payload`
- `status`
- `attempts`
- `published_at`
- `last_error`
- `next_attempt_at`

Important indexes:

- index on `(status, created_at)` for pending event batch selection
- index on `(status, next_attempt_at, created_at)` for retry scans

### `operation_idempotency`

Purpose:

- persist replayable responses for keyed release and order-status mutations

Important fields:

- `operation_type`
- `resource_id`
- `operation_value`
- `idempotency_key`
- `response_payload`

Important constraints:

- unique `(operation_type, resource_id, idempotency_key)`
- unique `(operation_type, resource_id, operation_value)`

### `channel_sync_attempt`

Purpose:

- persist outbound channel sync work derived from local outbox events

Important fields:

- `outbox_event_id`
- `channel`
- `sku`
- `event_type`
- `payload`
- `available_qty`
- `reserved_qty`
- `sold_qty`
- `status`
- `failure_type`
- `attempts`
- `next_attempt_at`

Important constraints:

- unique `(outbox_event_id, channel)`

### `channel_inventory_snapshot`

Purpose:

- persist the last known synced inventory fact per channel and SKU

Important fields:

- `channel`
- `sku`
- `available_qty`
- `reserved_qty`
- `sold_qty`
- `source_outbox_event_id`
- `synced_at`

Important constraints:

- unique `(channel, sku)`

### `inventory_reconciliation_run`

Purpose:

- record each operator-triggered reconciliation pass

Important fields:

- `scanned_sku_count`
- `scanned_snapshot_count`
- `open_drift_count`
- `completed_at`

### `inventory_reconciliation_drift`

Purpose:

- persist mismatches between central inventory and channel snapshots

Important fields:

- `run_id`
- `channel`
- `sku`
- central and observed quantity columns
- `status`
- `resolution_note`
- `resolved_at`

## Current Entity Relationships

- one `inventory_item` may have many `flash_sale_campaign` rows over time
- one `inventory_item` may have many `stock_reservation` rows
- one `flash_sale_campaign` may have many reservations
- one `stock_reservation` may produce one `order_header`
- all reservation and order lifecycle changes may produce many `outbox_event` rows
- one outbox event may produce many `channel_sync_attempt` rows, one per target channel
- one channel and SKU pair has one latest `channel_inventory_snapshot`
- one reconciliation run may produce many reconciliation drifts

## Current Enums And State-Carrying Fields

- reservation status: `ACTIVE`, `CONFIRMED`, `RELEASED`, `EXPIRED`
- campaign status: `DRAFT`, `ACTIVE`, `ENDED`
- order status: `PENDING`, `PAID`, `SHIPPED`
- outbox status: `PENDING`, `PUBLISHED`, `FAILED`
- channel sync status: `PENDING`, `SYNCED`, `FAILED`
- channel sync failure type: `TRANSIENT`, `PERMANENT`
- reconciliation drift status: `OPEN`, `RESOLVED`
- sales channel: `WEB`, `APP`, `SHOPEE`

## Target Data Model Gaps

Not yet implemented but likely future additions:

- connector-specific credential or cursor state
- order partitioning strategy or archive model

These are target-only ideas and are not part of the current schema.
