# 2026-03-15 Alert Delivery And Benchmark Automation

## What Changed

- the app now supports generic webhook-based external alert delivery with persisted delivery state and scheduled transition/reminder dispatch
- the benchmark runner now supports auto-promotion, optional commit override, generated `summary.md` and `comparison.json`, and a durable evidence catalog in `testing/k6/evidence/index.json`

## Evidence

- `.\mvnw --% test` passed on 2026-03-15
- `powershell -ExecutionPolicy Bypass -File .\testing\k6\Run-BenchmarkSuite.ps1 -ValidateFixtures` passed

## How To Reuse This Next Session

- read `docs/03_implementation/2026-03-15-alert-delivery-and-benchmark-automation-slice.md` before extending alert delivery or benchmark automation
- read `docs/04_audit_remediation/2026-03-15-evidence-gated-scale-audit.md` before proposing replication, partitioning, or service decomposition
