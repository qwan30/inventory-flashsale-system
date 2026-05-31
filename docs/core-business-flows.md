# Core Business Flows

**Last Updated:** 2026-05-30

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

1. Scheduler first resets retryable failed rows whose `next_attempt_at` is due.
2. Scheduler loads pending outbox rows by creation order.
3. Each row is wrapped in an `OutboxEnvelope` with `eventVersion`.
4. Message is published to Kafka topic `inventory-flashsale.events`.
5. Event is marked `PUBLISHED` on success.
6. Publish failure marks the row `FAILED`, stores `last_error`, increments `attempts`, and schedules `next_attempt_at` until max attempts is reached.

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
2. The system compares central inventory with each channel that can return an inbound snapshot.
3. `WEB` and `APP` read persisted snapshots when present.
4. `SHOPEE` and `TIKTOK_SHOP` read persisted snapshots in mock mode or live remote stock in real mode.
5. Open drifts are created or refreshed for mismatches.
6. Operators may list recent runs, list open drifts, and resolve open drifts through the admin or ops remediation surface.

### 10. Outbound Channel Sync

Current path:

1. Reservation reserve, confirm, release, and expire flows record a versioned outbox event.
2. The reservation application service schedules channel sync attempts for every `SalesChannel`: `WEB`, `APP`, `SHOPEE`, and `TIKTOK_SHOP`.
3. Order status updates schedule a channel sync attempt for the order's own channel.
4. `ChannelSyncScheduler` first resets retryable transient failures whose `next_attempt_at` is due.
5. Pending channel sync attempts are published through the registered port for their channel.
6. Successful inventory-bearing attempts upsert `channel_inventory_snapshot`.
7. Permanent failures and exhausted transient failures stay visible through ops alerts and channel health.

### 11. TikTok Signed Ingress

Current inventory path:

1. TikTok calls `POST /api/v1/channel-ingress/tiktok/inventory`.
2. Request must include `X-TikTok-Timestamp` and `X-TikTok-Signature`.
3. `TikTokIngressSignatureVerifier` validates the HMAC signature and five-minute timestamp skew.
4. `TikTokIngressService` deduplicates by `TIKTOK_SHOP:INVENTORY:{receiptId}`.
5. A `channel.inventory.ingested` outbox event is recorded as the snapshot source.
6. The `TIKTOK_SHOP` `channel_inventory_snapshot` is updated with the observed quantities.
7. A `channel_ingress_receipt` records the payload hash, receipt type, outcome, and processed time.

Current order-status path:

1. TikTok calls `POST /api/v1/channel-ingress/tiktok/orders/status`.
2. Signature and timestamp validation use the same ingress verifier.
3. `TikTokIngressService` deduplicates by `TIKTOK_SHOP:ORDER_STATUS:{receiptId}`.
4. The external status is normalized to `OrderStatus`.
5. `OrderApplicationService.updateStatus(...)` applies the central order transition and idempotency rules.
6. A `channel_ingress_receipt` records the processed ingress.

Current admin replay path:

1. Admin/operator calls `POST /api/v1/admin/channels/tiktok/ingress/replay`.
2. The replay request kind is either `INVENTORY` or `ORDER_STATUS`.
3. Replay delegates to the same inventory or order-status ingress service paths.
4. The controller records an `admin_activity_audit` entry with `TIKTOK_INGRESS_REPLAY_TRIGGERED`.

### 12. Channel Health And Operator Drill-Down

Current path:

1. Admin/operator authenticates through the admin auth flow.
2. `GET /api/v1/admin/channels/health` returns marketplace posture summaries for `SHOPEE` and `TIKTOK_SHOP`.
3. `GET /api/v1/admin/channels/health/{channel}` returns per-channel detail.
4. `OpsApplicationService` composes:
   - connector mode and configuration validity
   - sync backlog count
   - stale snapshot count
   - open reconciliation drift count
   - latest reconciliation run timing
   - latest failed channel sync detail
   - latest TikTok ingress receipt where applicable
   - latest TikTok replay summary from admin activity audit where applicable
5. Health is `UNAVAILABLE` for invalid real-mode configuration, `DEGRADED` for backlog/staleness/open drifts, and `HEALTHY` otherwise.

## Current Gaps

- central inventory remains the source of truth; TikTok inventory ingress updates channel snapshots, not central inventory quantities
- richer drift analytics and auto-remediation policy are still open
- connector credential/cursor persistence remains outside the current schema

## Related Current-State Docs

- `docs/system-map.md`
- `docs/retrieval-guide.md`
