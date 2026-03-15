# 2026-03-15 First Promoted Benchmark Baseline

## Summary

The benchmark evidence epic now has its first promoted baseline under:

- `testing/k6/evidence/20260315-133859-e2e3644/`

The repo's informational baseline target is now:

- `testing/k6/evidence/20260315-133859-e2e3644/report.json`

## What Changed

- promoted the first vetted K6 evidence set with `manifest.json`, `report.json`, and all per-scenario summary exports
- updated `testing/k6/suite.json` so later runs compare against the promoted baseline report
- tightened the promotion rule to require `suiteStatus = PASSED`, all scenario statuses `PASSED`, and `businessChecks.passed = true`
- fixed benchmark-script status handling so accepted reservation conflict outcomes are not counted as HTTP failures
- updated `testing/k6/lib/Report.ps1` to read current K6 summary exports that store metrics directly under each metric key
- enabled Java compiler parameter metadata in the parent Maven build so clean Spring MVC builds do not fail request binding
- replaced the stale local Kafka image tag in `docker-compose.yml` with a working Apache Kafka 3.9.1 KRaft setup while preserving the app's host bootstrap contract

## Evidence

Promoted baseline snapshot:

- hot-sku contention: average request duration about `164ms`, p95 about `920ms`, failed-request rate `0`
- flash sale window: average request duration about `70ms`, p95 about `220ms`, failed-request rate `0`
- reservation expiry: average request duration about `48ms`, p95 about `176ms`, failed-request rate `0`
- outbox backlog recovery: average request duration about `6ms`, p95 about `8ms`, failed-request rate `0`
- reconciliation load: average request duration about `8ms`, p95 about `26ms`, failed-request rate `0`

Operational invariants captured in the promoted report:

- inventory remained `available=100, reserved=0, sold=0`
- outbox backlog remained `pending=0, failed=0, retryableFailed=0`
- ops alerts and reconciliation drift endpoints remained reachable with no active drift or backlog breach

## Reuse Next Session

- start with `testing/k6/README.md` for the benchmark operator flow and current vetting rule
- treat `testing/k6/evidence/20260315-133859-e2e3644/report.json` as the current informational baseline for comparison
- if a later run beats these numbers, promote a new evidence set first, then update `suite.json.baselineTarget`
