# 2026-05-30 GitNexus Codebase Function Scan

## Scope

- Performed a read-only bootstrap and whole-function discovery pass for `inventory-flashsale-system`.
- Classified the work as a standard feature-sized discovery task because it spans backend modules, admin UI, docs, tests, and operational workflows, but does not change production code.
- Used `prompt-leverage`, `khuym:bootstrap-project-context`, repo docs, GitNexus CLI, and representative source inspection.

## GitNexus Baseline

- `npx.cmd gitnexus status` reported the index up to date.
- Indexed commit: `401a502`.
- Current commit: `401a502`.
- `npx.cmd gitnexus list` reported `422` files, `3264` symbols, `8923` edges, `143` clusters, and `258` processes for `inventory-flashsale-system`.
- GitNexus MCP tools were not exposed in this Codex session, so the local CLI was used with `-r inventory-flashsale-system`.

## Current Functional Map

- Core reservation and inventory correctness lives in `ReservationApplicationService`, `InventoryItem`, `StockReservation`, `FlashSaleCampaign`, and `RedisLockManager`.
- Reservation create, confirm, release, and expiry are idempotent/concurrency-sensitive flows that combine channel validation, SKU Redis locking, database state changes, outbox events, and channel sync scheduling.
- Order lifecycle is centralized in `OrderApplicationService` and `OrderHeader`; supported status flow remains `PENDING -> PAID -> SHIPPED` with idempotency records for repeated status updates.
- Outbox eventing is implemented in `OutboxService`, `OutboxEvent`, `OutboxEnvelope`, `OutboxPublisherScheduler`, and contract assets under `testing/contracts`.
- Omnichannel behavior includes outbound channel sync, persisted channel inventory snapshots, transient/permanent retry handling, reconciliation runs, and drift resolution through `ChannelSyncService`, `ChannelReconciliationService`, and `OpsApplicationService`.
- Marketplace integration includes Shopee and TikTok real-mode connector classes plus signed TikTok ingress and admin replay support through `TikTokIngressService` and related controllers.
- Admin/operator workflows include auth, refresh tokens, audit records, campaign management, ops remediation, channel health, benchmark evidence reads, and advisory ops copilot.
- Admin UI in `apps/admin-ui` is a Vite React app with protected routes for campaigns, ops, channel health, benchmark evidence, and ops copilot workflows.
- Runtime dependencies are MySQL, Redis, and Kafka through `docker-compose.yml`; benchmark evidence is controlled by the K6 suite and promoted evidence index.

## Best Next Reads

- Reservation or oversell work: `docs/retrieval-guide.md`, then `ReservationApplicationService`, `InventoryItem`, `FlashSaleCampaign`, and `ReservationFlowIntegrationTest`.
- Ops or channel work: `OpsApplicationService`, `ChannelSyncService`, `ChannelReconciliationService`, `TikTokIngressService`, and `apps/admin-ui/src/views/channels/ChannelHealthPage.tsx`.
- Admin UI work: `apps/admin-ui/src/App.tsx`, `apps/admin-ui/src/lib/api.ts`, and the route-specific view tests.
- Deployment or proof work: `README.md`, `docker-compose.yml`, `.github/workflows/ci.yml`, `testing/k6/README.md`, and `testing/k6/evidence/index.json`.

## Verification

- Read required orientation docs: `AGENTS.md`, `README.md`, `docs/00_index.md`, `docs/system-map.md`, and `docs/retrieval-guide.md`.
- Ran GitNexus status/list/query/context commands scoped to `inventory-flashsale-system`.
- Inspected representative source, API mappings, schedulers, migrations, admin UI routes/API client, testing assets, Docker Compose, and CI workflow.
- Did not run Maven, npm, Playwright, or K6 tests because this session was a read-only discovery plus documentation closeout.
