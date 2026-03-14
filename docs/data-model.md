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

Important indexes:

- index on `(status, created_at)` for pending event batch selection

## Current Entity Relationships

- one `inventory_item` may have many `flash_sale_campaign` rows over time
- one `inventory_item` may have many `stock_reservation` rows
- one `flash_sale_campaign` may have many reservations
- one `stock_reservation` may produce one `order_header`
- all reservation and order lifecycle changes may produce many `outbox_event` rows

## Current Enums And State-Carrying Fields

- reservation status: `ACTIVE`, `CONFIRMED`, `RELEASED`, `EXPIRED`
- campaign status: `DRAFT`, `ACTIVE`, `ENDED`
- order status: `PENDING`, `PAID`, `SHIPPED`
- outbox status: `PENDING`, `PUBLISHED`, `FAILED`
- sales channel: `WEB`, `APP`, `SHOPEE`

## Target Data Model Gaps

Not yet implemented but likely future additions:

- reconciliation result records
- channel sync audit trail
- explicit retry scheduling metadata for failed outbox rows
- order partitioning strategy or archive model

These are target-only ideas and are not part of the current schema.
