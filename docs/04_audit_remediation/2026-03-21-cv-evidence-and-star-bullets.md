# 2026-03-21 CV Evidence And STAR Bullets

## Scope

This note qualifies which project claims are safe to use in a resume or CV based on evidence already committed in the repository.

No new benchmark rerun was executed in this session.
All numbers below come from promoted K6 evidence, recorded verification notes, or current test inventory in the workspace.

## Evidence Reviewed

- promoted benchmark index:
  `testing/k6/evidence/index.json`
- promoted benchmark manifest and report:
  `testing/k6/evidence/20260315-133859-e2e3644/manifest.json`
  `testing/k6/evidence/20260315-133859-e2e3644/report.json`
- promoted scenario summaries:
  `testing/k6/evidence/20260315-133859-e2e3644/hot-sku-contention.summary.json`
  `testing/k6/evidence/20260315-133859-e2e3644/flash-sale-window.summary.json`
  `testing/k6/evidence/20260315-133859-e2e3644/reservation-expiry.summary.json`
  `testing/k6/evidence/20260315-133859-e2e3644/outbox-backlog-recovery.summary.json`
  `testing/k6/evidence/20260315-133859-e2e3644/reconciliation-load.summary.json`
- benchmark workflow and evidence contracts:
  `testing/k6/README.md`
  `docs/03_implementation/2026-03-15-benchmark-evidence-program.md`
- recorded verification notes:
  `docs/03_implementation/2026-03-15-v1-completion-wave.md`
  `docs/03_implementation/2026-03-16-production-hardening-channel-health-and-e2e.md`
  `docs/04_audit_remediation/2026-03-16-idea-02-progress-checklist.md`
- target guardrails:
  `docs/non-functional-requirements.md`
- current UI and backend test inventory:
  `apps/admin-ui/e2e/admin-workflows.spec.ts`
  `apps/admin-ui/src/**/*.test.tsx`
  `apps/api/src/test/java/**/*IntegrationTest.java`

## Verified Claims Safe For CV

### Correctness Under Load

- the promoted K6 suite passed `5/5` scenarios
- `businessChecks.passed = true`
- each promoted scenario reported `checksRate = 1` and `httpReqFailedRate = 0`
- post-run invariants passed:
  - `inventory_non_negative`
  - `inventory_stock_conservation`
  - outbox backlog endpoint healthy
  - alerts endpoint healthy
  - reconciliation drifts endpoint healthy
- the promoted seed and end-state stayed consistent at `sum=100, expected=100`

### Performance Numbers Safe To Quote

- hot SKU contention:
  - about `185.75 req/s`
  - `50` VUs
  - `avg 164.13ms`
  - `p95 920.36ms`
- flash-sale window:
  - `avg 70.12ms`
  - `p95 220.26ms`
- reservation expiry:
  - `avg 48.33ms`
  - `p95 175.51ms`
- outbox backlog recovery:
  - `avg 5.57ms`
  - `p95 8.34ms`
- reconciliation load:
  - `avg 8.26ms`
  - `p95 25.59ms`
- final ops snapshot after the promoted suite:
  - outbox `pending=0`
  - outbox `failed=0`
  - retryable failed outbox events `=0`
  - open reconciliation drifts `=0`

### Verification Coverage Safe To Quote

- admin UI browser coverage includes `7` Playwright workflows:
  - login and campaign access
  - refresh-cookie bootstrap
  - operator route redirect
  - outbox retry plus reconciliation trigger and drift resolve
  - benchmark detail drill-down
  - channel-health route
  - ops copilot panel
- admin UI includes `6` page-level test files:
  - `App.test.tsx`
  - `BenchmarkPages.test.tsx`
  - `CampaignRoutes.test.tsx`
  - `ChannelHealthPage.test.tsx`
  - `OpsPage.test.tsx`
  - `OpsRemediationPage.test.tsx`
- backend currently contains `9` concrete `*IntegrationTest` suites plus the shared `AbstractIntegrationTest` base harness
- the repo also has a recorded full `.\mvnw test` success on `2026-03-15`, while a later `2026-03-16` workspace could not reproduce it because Docker/Testcontainers were unavailable

## Claims That Must Stay Qualified

Do not write these in a CV unless fresh evidence is produced:

- do not claim the system sustains `1000 orders/second`
  - this is a target in `docs/non-functional-requirements.md`, not a proven result yet
- do not claim a global `p95 < 200ms`
  - the promoted hot-SKU contention scenario has `p95 920.36ms`
- do not claim backend integration proof is environment-independent
  - the current verification story still depends on Docker/Testcontainers
- do not say there are `10` backend integration suites unless the abstract base class is included in that count

## Resume-Ready STAR Bullets

These bullets are safe to translate into Vietnamese or English as long as the numbers stay unchanged.

- Built a flash-sale reservation engine around SKU locking and idempotency keys to prevent overselling; on the promoted K6 benchmark suite the system achieved `5/5` passing scenarios, `100%` business checks, `0%` HTTP failure, and preserved stock at `100/100` under hot-SKU load of about `186 req/s`.
- Designed and verified correctness-first flows for reservation expiry, flash-sale windows, outbox backlog recovery, and reconciliation; the promoted benchmark evidence recorded `avg 48-70ms` for the main business flows and `avg 5.6-8.3ms` for ops endpoints, with backlog and drift both returning to `0` after the suite.
- Delivered admin and ops workflows for campaign management, outbox retry, reconciliation, channel health, and ops copilot; verified the surface with `7` Playwright workflows, `6` UI test files, and `9` backend integration suites on a shared Testcontainers harness.

## Replacement Rules For Future Reruns

Only replace the bullets above after a fresh rerun on a machine with working Docker/Testcontainers and all of the following hold:

- `.\mvnw test` passes
- `npm test`, `npm run build`, and `npm run test:e2e` pass
- `.\testing\k6\Run-BenchmarkSuite.ps1 -BaseUrl http://localhost:8080 -SpringProfile benchmark -PromoteIfPassed` produces:
  - `suiteStatus = PASSED`
  - every scenario status `= PASSED`
  - `businessChecks.passed = true`
  - no oversell
  - no stock conservation drift

If the rerun is slower, incomplete, or fails any invariant, keep the current CV bullets unchanged.
