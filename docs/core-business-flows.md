# Core Business Flows

**Last Updated:** 2026-03-15

## Current Implemented Flows

### 1. Reserve Inventory During Flash Sale

Current path:

1. Client calls `POST /api/v1/flash-sales/{campaignId}/reservations`.
2. Request must include `X-Idempotency-Key`.
3. Channel validation runs first.
4. A Redis SKU lock is acquired.
5. Inside a transaction:
   - existing idempotent reservation is returned if found
   - campaign window and quota are validated
   - inventory moves from available to reserved
   - a reservation is created with expiry time
   - an outbox event `inventory.reservation.created` is recorded

Result:

- reservation becomes `ACTIVE`
- inventory reserved quantity increases
- available quantity decreases

### 2. Confirm Reservation

Current path:

1. Client calls `POST /api/v1/reservations/{reservationId}/confirm`.
2. Request must include `X-Idempotency-Key`.
3. Reservation is used to locate the SKU.
4. The same Redis SKU lock is acquired.
5. Inside a transaction:
   - same-key duplicate confirm returns the existing order
   - expired reservations are rejected
   - inventory moves from reserved to sold
   - campaign reserved quota moves to sold quota
   - a pending order is created or reused
   - reservation becomes `CONFIRMED`
   - outbox event `order.created` is recorded

### 3. Release Reservation

Current path:

1. Client calls `POST /api/v1/reservations/{reservationId}/release`.
2. Reservation is used to locate the SKU.
3. The Redis SKU lock is acquired.
4. Inside a transaction:
   - repeated release or expiry returns the current reservation status
   - confirmed reservations cannot be released
   - inventory moves from reserved back to available
   - campaign reserved quota is decreased
   - reservation becomes `RELEASED`
   - outbox event `inventory.reservation.released` is recorded

### 4. Expire Reservation

Current path:

1. Scheduler queries expired reservations still in `ACTIVE`.
2. For each one, the system reuses the release path with final state `EXPIRED`.
3. Inventory is returned to available quantity.
4. Campaign reserved quota is decreased.
5. An outbox release event is recorded.

### 5. Update Order Status

Current path:

1. Client calls `POST /api/v1/orders/{orderId}/status`.
2. Inside a transaction:
   - order is loaded
   - transition is validated
   - order status changes
   - matching outbox event such as `order.paid` or `order.shipped` is recorded

### 6. Publish Outbox Event

Current path:

1. Scheduler loads pending outbox rows by creation order.
2. Each row is wrapped in an outbox envelope.
3. Message is published to Kafka topic `inventory-flashsale.events`.
4. Event is marked `PUBLISHED` on success or `FAILED` on publish error.

### 7. Manage Flash Sale Campaigns

Current path:

1. Admin client authenticates through `POST /api/v1/admin/auth/login`.
2. Admin calls `POST /api/v1/admin/campaigns` to create a `DRAFT` campaign for an existing SKU.
3. Admin may update the draft with `PUT /api/v1/admin/campaigns/{campaignId}`.
4. Admin activates the campaign through `POST /api/v1/admin/campaigns/{campaignId}/activate`.
5. Admin may end a campaign through `POST /api/v1/admin/campaigns/{campaignId}/end`.
6. Each create, update, activate, and end action records an immutable audit entry.

### 8. Admin Or Operator Ops Remediation

Current path:

1. Admin or operator authenticates through the admin auth flow.
2. Client calls `GET /api/v1/admin/ops/alerts` or `GET /api/v1/admin/ops/outbox/backlog` to inspect current issues.
3. Operator-triggered actions such as outbox retry or reconciliation run and drift resolution are invoked through the `/api/v1/admin/ops/**` namespace.
4. Each mutating remediation action records an immutable admin activity audit entry.

### 9. Inventory Reconciliation

Current path:

1. Scheduler or operator triggers reconciliation.
2. The system compares central inventory with the latest channel snapshots or live Shopee reads in real mode.
3. Open drifts are created or refreshed for mismatches.
4. Operators may resolve open drifts through the admin or ops remediation surface.

## Target Future Flows

### Omnichannel Sync

Target future path:

- channel-originated orders, inventory changes, or fulfillment updates enter through a bounded integration flow
- the system normalizes them into central inventory and order semantics
- central inventory remains the source of truth

Current gap:

- central outbound sync, persisted snapshots, and Shopee live reads are implemented
- inbound marketplace orders and non-Shopee real connectors are still not implemented

### Inventory Reconciliation

Target future path:

- scheduled or operator-triggered reconciliation compares channel-side state with central inventory and order facts
- mismatches are reported for review or remediation

Current gap:

- scheduled and manual reconciliation now exist inside the monolith
- richer drift analytics, auto-remediation policy, and second-marketplace comparison are still open

## Related Current-State Docs

- `docs/system-map.md`
- `docs/retrieval-guide.md`
