# 2026-08-17 — Portfolio Review Milestone

Date: 2026-08-17

What changed: completed a full-codebase review of `inventory-flashsale-system` and collected verified evidence to upgrade the project's `portfolio` repo entry from a weak "research prototype" positioning to a correctness-first omnichannel inventory & flash-sale backend.

Evidence:
- Fixed the broken frontend CI lint step (`apps/admin-ui` had no `lint` script; added oxlint + config, verified `npm run lint` exits 0).
- Corrected README discrepancies: React 18 → 19, removed the false `flashsale -> inventory` dependency edge, corrected test-count claims.
- Collected traceable numbers (27 backend test files / 75 methods, 10 migrations, 15 tables, 39 endpoints, 5 promoted k6 scenarios) and the single promoted k6 run `20260315-133859-e2e3644` (hot-SKU p95 920.36 ms, 0% failure, stock conservation held).
- Recorded a remediation backlog for out-of-scope items: committed demo secrets, Redis lock watchdog, outbox multi-instance double-send, `int` quantities, reconciliation/snapshot consistency.

What future sessions should do with this:
- Treat the portfolio entry ("Inventory & Flash Sale System") as correctness-first, DB-backed reservations — intentionally complementary to the featured "Flash Sale Concurrency Engine" (throughput-first Lua pre-gating), not a competing narrative.
- Before claiming any latency improvement, note there is **no baseline/final comparison** — only a single promoted k6 run. Do not claim "production-ready" or "1000 orders/sec".
- The `junit-platform.properties` "duplicate" is a transitive test-classpath warning, not a tracked repo file — do not hunt for it again.
