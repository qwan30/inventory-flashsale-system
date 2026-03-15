# Inventory Flash Sale System Map

## Summary

This repository is a Java 21 + Spring Boot 3 modular monolith built around inventory correctness for flash sale reservations. The app layer in `apps/api` orchestrates thin HTTP entrypoints over bounded modules in `modules/`, with MySQL for state, Redis for distributed SKU locks, and Kafka for outbox-driven event publication.

Verified baseline on 2026-03-15:

- `.\mvnw test` passes.
- Integration tests use Testcontainers for MySQL, Redis, and Kafka.
- Demo seed data includes campaign `campaign-demo-001` and SKU `SKU-DEMO-001`.

## Runtime Surface

- `apps/api` is the deployable Spring Boot application.
- `modules/common` provides shared exceptions, time abstraction, and API error handling.
- `modules/channel` validates reservation requests per sales channel.
- `modules/flashsale` owns campaign window and quota rules.
- `modules/inventory` owns stock arithmetic, reservations, and optimistic locking.
- `modules/order` owns order status transitions.
- `modules/outbox` owns durable event recording and Kafka publication.

Public HTTP endpoints:

- `POST /api/v1/flash-sales/{campaignId}/reservations`
- `POST /api/v1/reservations/{reservationId}/confirm`
- `POST /api/v1/reservations/{reservationId}/release`
- `GET /api/v1/inventory/{sku}`
- `POST /api/v1/orders/{orderId}/status`
- `POST /api/v1/admin/auth/login`
- `POST /api/v1/admin/auth/refresh`
- `POST /api/v1/admin/auth/logout`
- `GET /api/v1/admin/campaigns`
- `POST /api/v1/admin/campaigns`
- `PUT /api/v1/admin/campaigns/{campaignId}`
- `POST /api/v1/admin/campaigns/{campaignId}/activate`
- `POST /api/v1/admin/campaigns/{campaignId}/end`
- `GET /api/v1/admin/campaigns/{campaignId}/audits`
- `GET /api/v1/admin/ops/alerts`
- `GET /api/v1/admin/ops/outbox/backlog`
- `POST /api/v1/admin/ops/outbox/{eventId}/retry`
- `POST /api/v1/admin/ops/reconciliation/runs`
- `GET /api/v1/admin/ops/reconciliation/drifts`
- `POST /api/v1/admin/ops/reconciliation/{driftId}/resolve`

Main orchestration entrypoints:

- `ReservationApplicationService.reserve(...)`
- `ReservationApplicationService.confirm(...)`
- `ReservationApplicationService.release(...)`
- `ReservationApplicationService.expireReservation(...)`
- `OrderApplicationService.updateStatus(...)`
- `AdminCampaignApplicationService.createCampaign(...)`
- `AdminCampaignApplicationService.updateCampaign(...)`
- `AdminCampaignApplicationService.activateCampaign(...)`
- `AdminCampaignApplicationService.endCampaign(...)`
- `AdminAuthService.login(...)`
- `AdminAuthService.refresh(...)`
- `AdminAuthService.logout(...)`

Background jobs:

- `ReservationExpiryScheduler` scans expired active reservations every `30s` by default.
- `OutboxPublisherScheduler` publishes pending outbox rows every `5s` by default.
- `ChannelSyncScheduler` processes pending channel sync attempts every `10s` by default.
- `ReconciliationScheduler` runs scheduled inventory reconciliation every `60s` by default.
- `OpsAlertDispatchScheduler` evaluates and sends alert notifications every `30s` by default.

## End-to-End Flow

### Reserve

1. `ReservationController` receives the request and requires an `Idempotency-Key` header.
2. `ReservationApplicationService.reserve(...)` checks for an existing reservation by idempotency key before taking a lock.
3. `ChannelService` validates the channel, SKU presence, and positive quantity.
4. `RedisLockManager` acquires `lock:inventory:{sku}` before entering the transaction.
5. Inside the transaction, the service:
   - Re-checks idempotency.
   - Loads the campaign and validates that the SKU matches.
   - Verifies the campaign is active and has remaining quota.
   - Loads inventory and moves quantity from `availableQty` to `reservedQty`.
   - Persists a new active `StockReservation` with `expiresAt = now + reservation.ttl`.
   - Records `inventory.reservation.created` in the outbox.

### Confirm

1. `ReservationApplicationService.confirm(...)` loads the reservation to discover its SKU, then locks that SKU.
2. Inside the transaction, the service:
   - Returns the existing order immediately if the reservation is already confirmed with the same confirm idempotency key.
   - Rejects expired reservations.
   - Moves inventory quantity from `reservedQty` to `soldQty`.
   - If a campaign is attached, moves campaign quantity from `reservedQuota` to `soldQuota`.
   - Creates or reuses an `OrderHeader` in `PENDING`.
   - Marks the reservation `CONFIRMED`.
   - Records `order.created` in the outbox.

### Release And Expire

1. `release(...)` and `expireReservation(...)` both discover the SKU from the reservation and then take the same Redis SKU lock.
2. Inside the transaction, the service:
   - Returns immediately if the reservation is already `RELEASED` or `EXPIRED`.
   - Rejects release for `CONFIRMED` reservations.
   - Moves inventory quantity from `reservedQty` back to `availableQty`.
   - If a campaign is attached, decrements `reservedQuota`.
   - Marks the reservation `RELEASED` or `EXPIRED`.
   - Records `inventory.reservation.released` in the outbox.

### Order Status Update

1. `OrderController` forwards the request into `OrderApplicationService.updateStatus(...)`.
2. Inside a transaction, the service:
   - Loads the order.
   - Applies the allowed transition in `OrderHeader.transitionTo(...)`.
   - Persists the new status.
   - Records `order.paid` or `order.shipped` in the outbox.

### Outbox Publish

1. `OutboxService.record(...)` serializes payloads and stores them as `PENDING`.
2. `OutboxPublisherScheduler` calls `publishPendingEvents()`.
3. `OutboxService` reads the next batch ordered by creation time.
4. Each event is wrapped in an `OutboxEnvelope` and sent to Kafka topic `inventory-flashsale.events`.
5. Success marks the row `PUBLISHED`; failure marks it `FAILED` with the error string.

## Module Invariants

### Inventory

- `InventoryItem.reserve(...)` rejects insufficient stock.
- `InventoryItem.release(...)` rejects releasing more than reserved.
- `InventoryItem.confirm(...)` rejects confirming more than reserved.
- `InventoryItem` uses JPA optimistic locking via `@Version`.
- `StockReservation.idempotencyKey` is unique at the database level.
- Reservation states are explicit: `ACTIVE`, `CONFIRMED`, `RELEASED`, `EXPIRED`.

### Flash Sale

- A campaign must be `ACTIVE` and within `[startsAt, endsAt]` to reserve.
- Campaign SKU must match the requested SKU.
- `reserveQuota(...)` rejects when `quota - reservedQuota - soldQuota` is insufficient.
- `confirmQuota(...)` moves quantity from reserved quota to sold quota.

### Order

- Orders are created `PENDING`.
- Allowed transitions are only `PENDING -> PAID -> SHIPPED`.
- Repeating the same status is a no-op.
- Any other transition raises a conflict.

### Outbox

- Domain state changes are persisted before publish.
- Events are retried only if some later logic resets status to `PENDING`; current code marks publish failures as `FAILED`.
- Publish ordering is FIFO within the selected batch by `created_at`.

### Channel

- Reservation validation is delegated by `SalesChannel`.
- `WEB` and `APP` currently use persisted sync and inbound snapshot behavior.
- `SHOPEE` supports mock or real-mode transport, including live inbound reconciliation reads in real mode.

## Persistence And Infra

Configuration defaults from `apps/api/src/main/resources/application.yml`:

- MySQL is the primary store, with Flyway migrations and Hibernate `ddl-auto=validate`.
- Redis host defaults to `localhost:6379`.
- Kafka bootstrap server defaults to `localhost:9094`.
- Reservation TTL defaults to `10m`.
- Redis lock wait timeout defaults to `2s`.
- Redis lock lease timeout defaults to `5s`.
- Outbox publish batch size defaults to `50`.

Schema from `V1__baseline.sql`:

- `inventory_item`
- `flash_sale_campaign`
- `stock_reservation`
- `order_header`
- `outbox_event`

Important database constraints and indexes:

- `stock_reservation.idempotency_key` is unique.
- `order_header.reservation_id` is unique.
- `idx_stock_reservation_status_expires_at` supports expiry scanning.
- `idx_outbox_status_created_at` supports batch publish selection.

Seed data from `V2__seed_demo_data.sql`:

- `SKU-DEMO-001` starts with 100 available units.
- `campaign-demo-001` is active from 2026-01-01 through 2027-01-01 with quota 50.

## Behavioral Evidence

Covered by integration tests:

- Successful reserve + confirm flow.
- Duplicate reservation idempotency returns the same reservation.
- Reservation outside campaign window is rejected.
- Confirm idempotency returns the same order for the same confirm key and rejects a different second key.
- Oversell is prevented under concurrent reserve attempts.
- Expired reservations return stock to inventory.
- Outbox events publish exactly once per state change in the tested flow.

Covered by module tests:

- Inventory stock arithmetic.
- Campaign window and quota rules.
- Order transition rules.
- Outbox publish state updates.
- Channel validation.

## Current Follow-Ups

These were observed during validation but are not breaking the build:

- Flyway warns that MySQL 8.4 is newer than its tested support window.
- Multiple `junit-platform.properties` files are present on the test classpath.
- Mockito emits a future JDK dynamic-agent warning during tests.
