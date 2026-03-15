# 2026-03-15 Broad Execution Roadmap

## Classification

- Task size: `large refactor`
- Execution model: serial prep followed by phased implementation with bounded subagent ownership
- Architectural guardrails:
  - keep the system as a modular monolith
  - do not weaken inventory correctness, idempotency, or current shopper-facing APIs
  - treat scale decisions as evidence-gated outcomes, not default roadmap assumptions

## Objective

Close the current post-foundation roadmap gap with one broad execution plan that starts from the shipped monolith foundation, ops closure, promoted benchmark baseline, and Shopee real connector.

This roadmap sequences the remaining high-value work as:

1. serial docs catch-up
2. external alert delivery
3. benchmark evidence maturity
4. evidence-gated scale audit

## Current Ground Truth

Already shipped before this roadmap:

- persisted idempotency replay for release and order-status mutations
- scheduled reconciliation runs and in-app ops alerts
- first promoted K6 benchmark baseline under `testing/k6/evidence/20260315-133859-e2e3644/`
- Shopee real sandbox outbound sync and live inbound reconciliation reads

Still meaningfully open:

- no external alert delivery path exists yet
- benchmark promotion, comparison output, and evidence indexing still require operator/manual handling
- no evidence-backed audit has yet converted promoted benchmark data into explicit go/no-go architecture guidance

## Phase 0: Serial Prep

Single owner responsibilities before parallel execution:

- refresh stale canonical docs so they match the shipped Shopee connector and promoted benchmark baseline
- persist this roadmap in `docs/02_planning/`
- update `docs/00_index.md` so future sessions can discover the roadmap and current benchmark milestone quickly
- do not spawn workers until the baseline docs and scope boundaries are trustworthy

## Phase A: External Alert Delivery

Classification: `standard feature`

Single owner scope:

- `apps/api/**`
- supporting Flyway migration for app-owned operational persistence

Deliverables:

- `app.alerts.delivery.*` configuration surface with default-safe disabled behavior
- persistent `alert_delivery_state` keyed by alert code
- webhook-based external alert publisher
- `OpsAlertDispatchScheduler` that dispatches on state transitions and periodic reminders
- dispatch success and failure metrics
- fail-safe behavior where alert delivery never blocks reservation, sync, outbox, or reconciliation work

Verification targets:

- inactive-to-active dispatch occurs once
- unchanged active alert does not spam
- reminder resend respects configured interval
- active-to-inactive clear notification is sent
- failures are recorded without breaking core business paths
- disabled mode emits no outbound calls

## Phase B: Benchmark Evidence Maturity

Classification: `standard feature`

Parallel track boundaries after Phase A lands:

### Track 1: Benchmark Runner

Owner scope: `testing/k6/**`

Deliverables:

- `Run-BenchmarkSuite.ps1 -PromoteIfPassed`
- `Run-BenchmarkSuite.ps1 -CommitSha`
- automatic promoted evidence copy on fully passing suites
- generated `comparison.json` against `suite.json.baselineTarget`
- generated human-readable `summary.md`
- maintained `testing/k6/evidence/index.json`

### Track 2: Docs Contracts

Owner scope:

- `README.md`
- `testing/k6/README.md`
- root canonical docs that describe benchmark workflow

Deliverables:

- docs aligned to automated promotion behavior and evidence index semantics
- canonical docs updated to reflect the current informational baseline target and comparison outputs

Verification targets:

- passing suite with `businessChecks.passed=true` promotes correctly
- any failed scenario blocks promotion
- `comparison.json`, `summary.md`, and `testing/k6/evidence/index.json` are generated consistently
- docs and runner behavior agree on promotion rules and baseline semantics

## Phase C: Evidence-Gated Scale Audit

Classification: audit/remediation follow-up

Parallel analysis ownership:

### Track 1: Evidence Audit

- analyze promoted evidence sets and comparison outputs against:
  - `1000 orders/sec`
  - `<200ms` average latency
  - `0% oversell`

### Track 2: Architecture Audit

- inspect locking, inventory writes, outbox throughput, reconciliation cadence, and Kafka publication hotspots
- propose changes only when supported by promoted evidence

Deliverable:

- one audit/remediation record in `docs/04_audit_remediation/` with explicit go/no-go guidance for:
  - lock tuning
  - DB/index work
  - batching changes
  - replication
  - partitioning
  - staying on the modular monolith

## Connector Policy

- Do not open a new connector-delivery phase unless product requirements first add another `SalesChannel`.
- Current modeled channels remain `WEB`, `APP`, and `SHOPEE`.
- Shopee real-mode transport is the only real marketplace connector in the current roadmap; further connector work is maintenance unless the product scope expands.

## Completion Criteria

- roadmap persisted in `docs/02_planning/`
- stale canonical docs corrected before implementation work continues
- Phase A, Phase B, and Phase C each produce their own durable outputs in the correct docs bucket
- `docs/00_index.md` is updated whenever a new high-signal roadmap, implementation, or audit artifact is added
