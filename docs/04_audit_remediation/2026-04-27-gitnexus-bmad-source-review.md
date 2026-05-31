# GitNexus + BMAD Source Review

Date: 2026-04-27

## Scope

Reviewed the full root project source for `inventory-flashsale-system`, excluding local side repositories `everything-claude-code/` and `skills/`.

Tooling used:

- `prompt-leverage` internal pass before execution.
- GitNexus CLI after refreshing the stale index with `npx gitnexus analyze`.
- GitNexus graph queries and symbol context for reservation, outbox, channel sync, ops, admin auth, and ops copilot flows.
- BMAD-style review lens for business capability, module boundaries, architecture/invariants, and delivery readiness. No local BMAD runner or `.bmad` project assets were present in this repo.

GitNexus index after refresh:

- commit: `401a502`
- nodes: 3264
- edges: 8923
- flows: 258

## Implemented Capabilities

### Core Flash Sale And Inventory

- Flash sale reservation create, confirm, release, and expiry flows are implemented in `ReservationApplicationService`.
- Reservation creation enforces idempotency, channel validation, campaign SKU/window/quota checks, Redis SKU locking, inventory stock movement, outbox recording, and channel sync scheduling.
- Confirm moves inventory from reserved to sold, confirms campaign quota, creates or reuses a pending order, records `order.created`, and schedules channel sync.
- Release and expiry return stock to available inventory, release campaign quota, write release events, and preserve idempotent release behavior.
- Inventory arithmetic is centralized in `InventoryItem`, with optimistic locking through `@Version`.
- Reservation state is explicit through `StockReservation` and `ReservationStatus`.

Evidence:

- `apps/api/src/main/java/com/codex/flashsale/application/ReservationApplicationService.java`
- `modules/inventory/src/main/java/com/codex/flashsale/inventory/InventoryItem.java`
- `modules/inventory/src/main/java/com/codex/flashsale/inventory/StockReservation.java`
- `modules/flashsale/src/main/java/com/codex/flashsale/flashsale/FlashSaleCampaign.java`
- `apps/api/src/test/java/com/codex/flashsale/ReservationFlowIntegrationTest.java`
- `apps/api/src/test/java/com/codex/flashsale/ReservationExpiryIntegrationTest.java`

### Order Lifecycle

- Order status update is implemented with allowed transition enforcement.
- Current lifecycle supports `PENDING -> PAID -> SHIPPED`; repeat of the same status is a no-op and invalid transitions conflict.
- Order status updates can be idempotently replayed through `OperationIdempotencyService`.
- Paid and shipped transitions emit versioned outbox events.

Evidence:

- `apps/api/src/main/java/com/codex/flashsale/application/OrderApplicationService.java`
- `modules/order/src/main/java/com/codex/flashsale/order/OrderHeader.java`
- `apps/api/src/test/java/com/codex/flashsale/ReservationFlowIntegrationTest.java`

### Outbox And Event Contracts

- Durable outbox persistence and scheduled Kafka publication are implemented.
- Outbox rows support `PENDING`, `PUBLISHED`, and `FAILED` states, retry scheduling, manual retry reset, and event versioning.
- Published messages are wrapped in `OutboxEnvelope` and include `eventVersion`.
- JSON schema fixtures and contract simulator assets exist under `testing/contracts`.

Evidence:

- `modules/outbox/src/main/java/com/codex/flashsale/outbox/OutboxService.java`
- `modules/outbox/src/main/java/com/codex/flashsale/outbox/OutboxEvent.java`
- `modules/outbox/src/main/java/com/codex/flashsale/outbox/OutboxEnvelope.java`
- `apps/api/src/main/java/com/codex/flashsale/scheduler/OutboxPublisherScheduler.java`
- `testing/contracts/schemas/outbox-envelope.v1.schema.json`

### Omnichannel Sync And Reconciliation

- Sales channels include `WEB`, `APP`, `SHOPEE`, and `TIKTOK_SHOP`.
- Outbound channel sync attempts are persisted for all channels after inventory-affecting events.
- Channel sync supports retries for transient failures and permanent failure classification.
- Persisted channel inventory snapshots feed reconciliation.
- Manual and scheduled reconciliation runs create or refresh open drift records without auto-correcting inventory.
- Drift resolution APIs are implemented for operators.

Evidence:

- `modules/channel/src/main/java/com/codex/flashsale/channel/SalesChannel.java`
- `modules/channel/src/main/java/com/codex/flashsale/channel/sync/ChannelSyncService.java`
- `modules/channel/src/main/java/com/codex/flashsale/channel/reconciliation/ChannelReconciliationService.java`
- `apps/api/src/main/java/com/codex/flashsale/application/OpsApplicationService.java`
- `apps/api/src/test/java/com/codex/flashsale/OpsAndChannelIntegrationTest.java`

### Marketplace Connectors And TikTok Ingress

- Shopee has real-mode REST sync, signing support, and live inbound reconciliation reads behind config.
- TikTok Shop has real-mode REST sync, signing support, live inbound reconciliation reads, signed inventory ingress, signed order-status ingress, and replay support.
- TikTok inventory ingress updates channel snapshots without changing central inventory.
- TikTok order-status ingress normalizes into the central order lifecycle with an ingress idempotency key.

Evidence:

- `apps/api/src/main/java/com/codex/flashsale/connector/shopee/`
- `apps/api/src/main/java/com/codex/flashsale/connector/tiktok/`
- `apps/api/src/main/java/com/codex/flashsale/channel/sync/ShopeeLiveChannelInboundGateway.java`
- `apps/api/src/main/java/com/codex/flashsale/channel/sync/TikTokLiveChannelInboundGateway.java`
- `apps/api/src/main/java/com/codex/flashsale/channel/ingress/TikTokIngressService.java`
- `apps/api/src/main/java/com/codex/flashsale/controller/TikTokIngressController.java`
- `apps/api/src/test/java/com/codex/flashsale/ShopeeSandboxConnectorIntegrationTest.java`
- `apps/api/src/test/java/com/codex/flashsale/TikTokConnectorIntegrationTest.java`

### Admin, Security, And Operator Surfaces

- Admin auth is implemented with users, BCrypt password hashes, JWT access tokens, refresh tokens, and audit records.
- Browser refresh-cookie mode is supported through HttpOnly cookies.
- Role boundaries are present: campaign management is admin-only, ops/channel/benchmark/copilot surfaces are admin-or-operator.
- Campaign management supports list, detail, create, update draft, activate, end, and immutable audit reads.
- Ops surfaces support alert reads, outbox backlog/event reads, outbox retry, reconciliation run history, manual reconciliation, drift reads, and drift resolution.
- Channel health summary and per-channel detail APIs are implemented for marketplace posture.

Evidence:

- `apps/api/src/main/java/com/codex/flashsale/security/SecurityConfiguration.java`
- `apps/api/src/main/java/com/codex/flashsale/admin/AdminAuthService.java`
- `apps/api/src/main/java/com/codex/flashsale/admin/AdminRefreshTokenCookieSupport.java`
- `apps/api/src/main/java/com/codex/flashsale/application/AdminCampaignApplicationService.java`
- `apps/api/src/main/java/com/codex/flashsale/controller/AdminOpsController.java`
- `apps/api/src/main/java/com/codex/flashsale/controller/AdminChannelController.java`
- `apps/api/src/test/java/com/codex/flashsale/AdminSecurityIntegrationTest.java`
- `apps/api/src/test/java/com/codex/flashsale/AdminWorkflowApiIntegrationTest.java`

### Ops Copilot, Benchmark Evidence, And Admin UI

- Advisory-only ops copilot APIs are implemented with capabilities and analysis endpoints.
- Ops copilot builds bounded context from alerts, outbox, reconciliation, channel health, benchmark evidence, and campaign audits.
- Gemini provider integration exists behind config; service sanitizes unsupported links/citations and falls back to safe actions.
- Benchmark evidence APIs list, fetch latest, and fetch detail for promoted K6 evidence.
- React admin UI implements login/session bootstrap, campaigns, campaign detail/audit, ops overview, ops remediation, channel health, benchmark overview/detail, and ops copilot panel.
- Admin UI role routing redirects operators away from admin-only campaign routes.
- API and admin UI Dockerfiles plus GitHub Actions CI are present.

Evidence:

- `apps/api/src/main/java/com/codex/flashsale/ai/`
- `apps/api/src/main/java/com/codex/flashsale/benchmark/BenchmarkEvidenceService.java`
- `apps/admin-ui/src/App.tsx`
- `apps/admin-ui/src/lib/api.ts`
- `apps/admin-ui/src/views/`
- `apps/admin-ui/e2e/admin-workflows.spec.ts`
- `apps/api/Dockerfile`
- `apps/admin-ui/Dockerfile`
- `.github/workflows/ci.yml`

## Review Findings

### Medium: TikTok ingress endpoint is public while blank ingress secret is not guarded in default mode

`SecurityConfiguration` permits `/api/v1/channel-ingress/tiktok/**`, and request authentication depends entirely on `TikTokIngressSignatureVerifier`. `TikTokConnectorConfigurationValidator` requires `app.channel.tik-tok.ingress-secret` only when `app.channel.tik-tok.mode=real`. In the default mock/local mode, the ingress verifier can still be invoked with a blank or unset secret. That can produce server errors for signed ingress attempts or weaken assumptions around whether the endpoint is actually enabled.

Suggested remediation:

- Add a dedicated ingress enablement/config validator independent of outbound connector `real` mode.
- Reject blank ingress secrets with a controlled `401` or disable the ingress routes unless a secret is configured.
- Add a controller test for blank/unset ingress secret behavior.

Evidence:

- `apps/api/src/main/java/com/codex/flashsale/security/SecurityConfiguration.java`
- `apps/api/src/main/java/com/codex/flashsale/channel/ingress/TikTokIngressSignatureVerifier.java`
- `apps/api/src/main/java/com/codex/flashsale/config/TikTokConnectorConfigurationValidator.java`
- `apps/api/src/main/resources/application.yml`

### Medium: Release-grade backend proof remains Docker-blocked in this workspace

`.\mvnw test` compiles and passes module tests before failing in `apps/api` because Testcontainers cannot find a valid Docker environment. This blocks confirmation of integration tests for reservation concurrency, admin workflows, connectors, and reconciliation in the current machine.

Suggested remediation:

- Run full `.\mvnw test` on a machine with Docker Desktop or another valid Docker engine.
- Treat integration-test assertions as implemented but not freshly verified in this session.

Evidence:

- Full `.\mvnw test` on 2026-04-27: module tests passed; `apps/api` failed with `Could not find a valid Docker environment`.

### Low: Several durable docs lag behind the current source

Top-level docs are mostly accurate through 2026-03-16, but some current-state docs still say only Shopee has real connector maturity or list only `WEB`, `APP`, and `SHOPEE` as sales channels. Current source includes `TIKTOK_SHOP`, real-mode TikTok sync, live TikTok reads, and signed TikTok ingress.

Suggested remediation:

- Refresh `docs/system-modules.md`, `docs/data-model.md`, and `docs/core-business-flows.md` to match the current TikTok and channel-health source baseline.

Evidence:

- `modules/channel/src/main/java/com/codex/flashsale/channel/SalesChannel.java`
- `docs/system-map.md`
- `docs/system-modules.md`
- `docs/data-model.md`
- `docs/core-business-flows.md`

## Verification

Commands run:

- `npx gitnexus status` initially reported a stale index.
- `npx gitnexus analyze` refreshed the index successfully.
- `npx gitnexus status` then reported the index up to date.
- `npx gitnexus query ...` and `npx gitnexus context ...` were used for core flows and symbols.
- `.\mvnw test` failed only after module tests passed, at `apps/api` Testcontainers startup because Docker was unavailable.
- `.\mvnw -pl apps/api -am -DskipTests compile` passed.
- `npm test` in `apps/admin-ui` passed: 6 files, 25 tests.
- `npm run build` in `apps/admin-ui` passed.
- `npm run test:e2e` in `apps/admin-ui` passed: 7 Playwright tests.

## Completion Summary

The repo has implemented a broad V1-style modular monolith: flash-sale reservation correctness, order lifecycle, durable eventing, channel sync/reconciliation, Shopee/TikTok connector paths, admin/operator APIs, benchmark evidence reads, ops copilot, and a tested React admin UI.

The main unclosed items are not missing source modules; they are operational proof and hardening:

- Docker-backed backend integration and benchmark evidence need a working container environment.
- TikTok ingress secret handling should be hardened when ingress routes are exposed.
- Some durable docs need a current-state refresh for TikTok/channel capabilities.
