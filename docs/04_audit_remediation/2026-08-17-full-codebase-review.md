# 2026-08-17 — Full Codebase Review & Portfolio Entry Upgrade

Date: 2026-08-17

Scope: full-codebase review of `inventory-flashsale-system` plus evidence collection for the `portfolio` repo entry rewrite.

## A. Suspect verification

| # | Suspect | Verdict | File / line |
|---|---|---|---|
| 1 | Frontend CI lint step broken | **CONFIRMED + FIXED** | `.github/workflows/ci.yml:129` ran `npm run lint`; `apps/admin-ui/package.json` had no `lint` script and no linter. Added `oxlint` devDependency, a `lint` script, and `.oxlintrc.json`. Verified `npm run lint` exits 0 (warnings only). |
| 2 | README "React 18" | **CONFIRMED + FIXED** | `README.md:10` badge said React 18; `apps/admin-ui/package.json` is React `^19.0.0`. Changed badge to React 19. |
| 3 | README `flashsale -> inventory` dependency | **CONFIRMED + FIXED** | `README.md:440` dependency diagram asserted `flashsale --> inventory`; no such edge exists (module `pom.xml` deps are only `common`). Removed the edge. |
| 4 | README test-count claims | **CONFIRMED + FIXED** | `README.md:403,407` claimed "75+ assertions" and "20 frontend spec files". Ground truth: 75 Java test **methods** across 27 files; 6 Vitest files (25 tests) + 1 Playwright spec (7 tests) = 32 cases across 7 files. Corrected the table. |
| 5 | Duplicate `junit-platform.properties` | **DISPUTED — not a duplicate file** | No `junit-platform.properties` exists in any `src/test/resources` tree. The warning recorded in `docs/system-map.md:275` ("Multiple junit-platform.properties on the test classpath") traces to a **transitive conflict** (multiple test deps bundling the file), not a tracked duplicate in the repo. No repo file to delete. Left intact; noted in backlog. |
| 6 | Flyway / MySQL 8.4 warning | **CONFIRMED (informational)** | `docs/system-map.md:274` — Flyway warns MySQL 8.4 is newer than its tested support window. Warning only; no build break. Not fixed (no action needed). |
| 7 | Committed demo secrets | **CONFIRMED (out of scope, backlog)** | Seeded demo credentials live in `V2__seed_demo_data.sql` and demo config. Per plan, secrets refactor is explicitly out of scope for this pass; recorded in remediation backlog below. |

## B. Fowler smell baseline (core flows)

Reviewed: `ReservationApplicationService`, `OutboxService`, `ChannelSyncService`, `OperationIdempotencyService`, `RedisLockManager`.

| Smell | Location | Note |
|---|---|---|
| Long method | `ReservationApplicationService` (reserve/confirm paths, Redis+tx+outbox orchestration inline) | Reserve touches idempotency, lock, quota, inventory, outbox, and sync in one method. Readable but dense; extract candidate. |
| Message chain / mixed layers | `ReservationApplicationService` → `RedisLockManager` → `StringRedisTemplate` | Lock acquisition/release leaks infra abstraction into application service. |
| Primitive obsession | Inventory quantities modeled as `int` (`InventoryItem.availableQty/reservedQty/soldQty`) | See Correctness risk below. |
| Duplicated envelope building | `OutboxService` versioned-envelope construction | Envelope shape repeated across event types; a factory would centralize it. |
| Feature envy | `ChannelSyncService` iterating `SalesChannel` values to fan out snapshots | Enum iteration belongs to the enum or a dedicated coordinator. |

These are **readability smells, not defects** — no change this pass (recorded for future cleanup).

## C. Correctness risks — remediation backlog (NOT fixed)

| Risk | Location | Interview probe |
|---|---|---|
| Redis lock lacks watchdog / auto-renewal | `RedisLockManager` (fixed TTL ~5s) | A transaction exceeding the TTL releases the lock early → two writers enter the critical section. Needs fencing token + renewal, or `SELECT FOR UPDATE` fallback. |
| Outbox multi-instance double-send | `OutboxPublisherScheduler` (poll PENDING → publish → mark PUBLISHED) | Two instances polling concurrently can both publish before either marks PUBLISHED. Safe only single-instance. Needs claim/lease on rows or a partition assignment. |
| `int` quantities (overflow) | `InventoryItem`, `OrderHeader` | `int` can overflow at ~2.1B units; also no currency-cent vs unit distinction for monetary-aligned quantities. |
| Reconciliation vs local snapshot consistency | `ChannelReconciliationService` | Drifts are surfaced but not auto-corrected; a stale local snapshot can mask real channel drift. Correctness depends on snapshot freshness. |

> Items here are deliberately recorded-only per plan (no edits). Fixes gated on future sessions.

## Verified numbers (for portfolio + docs)

| Metric | Value | Source |
|---|---|---|
| Backend test files | 27 (`*Test.java`) | `find modules apps/api -path "*src/test*" -name "*Test.java"` |
| Java test methods | 75 | `docs/04_audit_remediation/2026-06-07-project-evidence-sheet.md` (strict annotation count) |
| Vitest unit files / cases | 6 files / 25 tests | `apps/admin-ui/src/*.test.tsx` |
| Playwright e2e | 1 spec / 7 tests | `apps/admin-ui/e2e/admin-workflows.spec.ts` |
| Flyway migrations | 10 | `apps/api/src/main/resources/db/migration/V1..V10` |
| DB tables | 15 | migration `CREATE TABLE` count |
| REST endpoints | 39 | controller annotation count |
| Maven modules | 6 | `modules/{common,channel,flashsale,inventory,order,outbox}` |
| Scheduled jobs | 5 | scheduler package |
| k6 scripts | 8 | `testing/k6/*.js` (scenario scripts) |
| k6 promoted scenarios | 5 | `testing/k6/evidence/index.json` (single promoted run `20260315-133859-e2e3644`) |

## Promoted k6 evidence (single promoted run, commit `e2e3644`)

From `testing/k6/evidence/20260315-133859-e2e3644/report.json`:

| Scenario | avg | p95 | fail rate | post-run checks |
|---|---|---|---|---|
| hot-sku-contention | 164.13 ms | 920.36 ms | 0 | inventory_invariants, ops_snapshots |
| flash-sale-window | 70.12 ms | 220.26 ms | 0 | inventory_invariants, ops_snapshots |
| reservation-expiry | 48.33 ms | 175.51 ms | 0 | inventory_invariants, ops_snapshots |
| outbox-backlog-recovery | 5.57 ms | 8.34 ms | 0 | ops_snapshots |
| reconciliation-load | 8.26 ms | 25.59 ms | 0 | ops_snapshots |

Business invariants passed: `inventory_non_negative` (available=100), `inventory_stock_conservation` (sum=100=expected), backlog/alerts/reconciliation endpoints OK. Outbox backlog 0, all 6 alerts INACTIVE, 0 open drifts. **Hot-SKU test ran at 50 VUs / ~185 requests/sec over 5,692 iterations with 0 failed requests and stock conservation holding.**

## What changed (this pass)

- `README.md` — React 19 badge, removed false `flashsale -> inventory` edge, corrected test-count table.
- `apps/admin-ui/package.json` — added `lint` script + `oxlint` devDependency.
- `apps/admin-ui/.oxlintrc.json` — new oxlint config (react + typescript + oxc plugins).
- `docs/00_index.md` — index entries for this review + history note.
- `docs/05_history/2026-08-17-portfolio-review-milestone.md` — milestone note.
