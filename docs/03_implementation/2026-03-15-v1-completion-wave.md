# 2026-03-15 V1 Completion Wave

## Scope Delivered

This wave closes the largest remaining V1 gaps from the 2026-03-15 baseline without leaving the modular monolith.

Delivered in code:

- versioned outbox envelopes with persisted `event_version`
- event contract constants plus contract fixtures and simulator harness scaffolding under `testing/contracts`
- benchmark evidence read APIs for promoted K6 runs
- browser-safe admin refresh-cookie support while preserving the existing JSON refresh-token flow
- provider-aware alert delivery routing with generic webhook fallback plus Slack and PagerDuty publishers
- TikTok Shop as a fourth `SalesChannel` with mock/real sync seams
- TikTok real outbound sync, live inbound reconciliation reads, signed ingress APIs, idempotent ingress receipts, and admin replay support
- React admin/operator SPA scaffold under `apps/admin-ui`
- canonical docs truth-up for overview, system map, API contract, and UI roles

## Files And Boundaries

- inventory correctness, reservation locking, and order transition rules remain owned by the existing domain modules
- versioned wire-contract work stays centered in `modules/outbox` and `apps/api/src/main/java/com/codex/flashsale/events`
- TikTok connector and ingress work stays bounded to `modules/channel`, `apps/api` connector/ingress packages, and additive Flyway migrations
- admin product surface adds a new frontend app in `apps/admin-ui` and additive admin/reporting code in `apps/api`
- benchmark reporting remains read-only over promoted evidence; the runner stays CLI-driven in `testing/k6`

## Verification

Focused backend gate:

```powershell
.\mvnw --% -pl modules/outbox,apps/api -am test -Dtest=OutboxServiceTest,AdminSecurityIntegrationTest,AdminBenchmarkEvidenceIntegrationTest,AdminBrowserCookieAuthIntegrationTest,OpsAlertDeliveryServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Focused channel and connector gate:

```powershell
.\mvnw --% -pl modules/channel,apps/api -am test -Dtest=OpsAndChannelIntegrationTest,ShopeeSandboxConnectorIntegrationTest,TikTokConnectorIntegrationTest,ReservationFlowIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

Repository gate:

```powershell
.\mvnw test
```

Frontend gate:

```powershell
npm test
npm run build
```

Results:

- backend focused gate: `BUILD SUCCESS`
- channel and connector focused gate: `BUILD SUCCESS`
- repository gate: `BUILD SUCCESS`
- frontend test gate: `PASS`
- frontend production build: `PASS`

## Notes For Future Sessions

- TikTok ingress writes a synthetic outbox-backed source id so `channel_inventory_snapshot` keeps its foreign-key contract intact
- promoted benchmark evidence may not include `summary.md` or `comparison.json` for older runs, so the benchmark evidence API tolerates missing optional files
- the admin UI is intentionally a first operational shell; deeper UX polish, richer CRUD coverage, and deployment packaging remain follow-on work
- tests still emit known environment warnings for Flyway MySQL support window, multiple Kafka `junit-platform.properties`, Mockito dynamic-agent loading, and scheduler shutdown noise after Testcontainers teardown
