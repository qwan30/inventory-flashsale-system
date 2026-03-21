# Retrieval Guide

## Purpose

Use this guide to reduce repeated repo discovery. It points to the first places to read for common tasks and debugging paths.

## Where To Start

### Reservation Bugs

Read in this order:

- `apps/api/src/main/java/com/codex/flashsale/application/ReservationApplicationService.java`
- `modules/inventory/src/main/java/com/codex/flashsale/inventory/InventoryItem.java`
- `modules/inventory/src/main/java/com/codex/flashsale/inventory/StockReservation.java`
- `modules/flashsale/src/main/java/com/codex/flashsale/flashsale/FlashSaleCampaign.java`

Use this path for:

- insufficient stock
- idempotency mismatches
- release or confirm conflicts
- flash sale quota issues

### Oversell Or Concurrency Issues

Read in this order:

- `apps/api/src/main/java/com/codex/flashsale/config/RedisLockManager.java`
- `apps/api/src/main/java/com/codex/flashsale/application/ReservationApplicationService.java`
- `modules/inventory/src/main/java/com/codex/flashsale/inventory/InventoryItem.java`
- `apps/api/src/test/java/com/codex/flashsale/ReservationFlowIntegrationTest.java`

Look for:

- SKU lock scope
- lock timeout and lease settings
- optimistic locking conflicts
- assumptions in the concurrency integration test

### Expiry Behavior

Read in this order:

- `apps/api/src/main/java/com/codex/flashsale/scheduler/ReservationExpiryScheduler.java`
- `apps/api/src/main/java/com/codex/flashsale/application/ReservationApplicationService.java`
- `modules/inventory/src/main/java/com/codex/flashsale/inventory/StockReservation.java`
- `apps/api/src/test/java/com/codex/flashsale/ReservationExpiryIntegrationTest.java`

Look for:

- `ACTIVE` reservation query by expiry time
- transition to `EXPIRED`
- inventory restoration
- scheduler cadence versus TTL

### API Changes

Read in this order:

- `apps/api/src/main/java/com/codex/flashsale/controller/ReservationController.java`
- `apps/api/src/main/java/com/codex/flashsale/controller/InventoryController.java`
- `apps/api/src/main/java/com/codex/flashsale/controller/OrderController.java`
- `apps/api/src/main/java/com/codex/flashsale/api/`

Look for:

- endpoint shape
- request and response DTOs
- required headers such as `Idempotency-Key`

### Channel Health And Operator Posture

Read in this order:

- `apps/api/src/main/java/com/codex/flashsale/controller/AdminChannelController.java`
- `apps/api/src/main/java/com/codex/flashsale/application/OpsApplicationService.java`
- `modules/channel/src/main/java/com/codex/flashsale/channel/sync/ChannelSyncService.java`
- `modules/channel/src/main/java/com/codex/flashsale/channel/reconciliation/ChannelReconciliationService.java`
- `apps/api/src/main/java/com/codex/flashsale/channel/ingress/TikTokIngressService.java`
- `apps/admin-ui/src/views/channels/ChannelHealthPage.tsx`
- `apps/admin-ui/e2e/admin-workflows.spec.ts`

Use this path for:

- channel health endpoint changes
- per-channel drill-down changes
- degraded or unavailable marketplace posture
- stale snapshot or backlog aggregation
- replay summary visibility
- operator route regressions in the admin UI

### Ops Copilot

Read in this order:

- `apps/api/src/main/java/com/codex/flashsale/controller/AdminOpsCopilotController.java`
- `apps/api/src/main/java/com/codex/flashsale/ai/OpsCopilotService.java`
- `apps/api/src/main/java/com/codex/flashsale/ai/OpsCopilotContextService.java`
- `apps/api/src/main/java/com/codex/flashsale/ai/GeminiOpsCopilotProvider.java`
- `apps/admin-ui/src/views/ops/OpsCopilotPanel.tsx`

Use this path for:

- Gemini provider wiring
- advisory-only AI response shaping
- prompt context composition
- copilot UI regressions in the ops workflow

### Order Lifecycle Work

Read in this order:

- `apps/api/src/main/java/com/codex/flashsale/application/OrderApplicationService.java`
- `modules/order/src/main/java/com/codex/flashsale/order/OrderHeader.java`
- `modules/order/src/main/java/com/codex/flashsale/order/OrderService.java`

Look for:

- allowed status transitions
- event type mapping
- how order creation is coupled to reservation confirm

### Eventing Or Kafka Issues

Read in this order:

- `modules/outbox/src/main/java/com/codex/flashsale/outbox/OutboxService.java`
- `modules/outbox/src/main/java/com/codex/flashsale/outbox/OutboxEvent.java`
- `apps/api/src/main/java/com/codex/flashsale/scheduler/OutboxPublisherScheduler.java`
- `apps/api/src/test/java/com/codex/flashsale/ReservationFlowIntegrationTest.java`

Look for:

- pending-to-published or pending-to-failed transitions
- publish batch size
- topic name
- exactly-once expectations in tests versus at-least-once realities in production

### Schema Or Migration Work

Read in this order:

- `apps/api/src/main/resources/db/migration/V1__baseline.sql`
- `apps/api/src/main/resources/db/migration/V2__seed_demo_data.sql`
- `apps/api/src/main/resources/application.yml`

Look for:

- uniqueness constraints
- foreign keys
- indexes that support schedulers
- demo assumptions used by README or manual testing

### Local Environment And Smoke Testing

Read in this order:

- `README.md`
- `docker-compose.yml`
- `testing/k6/`
- `apps/api/src/test/java/com/codex/flashsale/AbstractIntegrationTest.java`

Use this path for:

- bringing up MySQL, Redis, Kafka
- checking k6 scripts
- understanding the integration test infrastructure

## Fast Heuristics

- If the problem changes inventory numbers, start with `ReservationApplicationService` and `InventoryItem`.
- If the problem mentions campaign windows or quota, inspect `FlashSaleCampaign`.
- If the problem mentions channel posture or operator dashboards, start with `OpsApplicationService` and `ChannelHealthPage`.
- If the problem mentions advisory AI analysis for operators, start with `AdminOpsCopilotController`, `OpsCopilotService`, and `OpsCopilotPanel`.
- If the problem mentions duplicate requests, inspect reservation and confirm idempotency handling in `StockReservation` and `ReservationApplicationService`.
- If the problem mentions delayed or missing events, inspect `OutboxService` and scheduler timings in `application.yml`.
- If the problem mentions behavior under load, compare app logic with `ReservationFlowIntegrationTest` and the `testing/k6` scripts.
