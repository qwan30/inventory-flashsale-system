# Non-Functional Requirements

**Last Updated:** 2026-03-16

## Current Non-Functional Priorities

### Correctness

- inventory correctness is the top priority
- no accepted change should weaken stock integrity under concurrency

### Consistency

- central inventory must remain the source of truth
- reservation, campaign, order, and outbox state must stay coherent within application-service orchestration

### Idempotency

- reserve and confirm flows rely on idempotency keys
- repeated operations should not create duplicate business side effects

### Observability

Current implemented signals include:

- correlation IDs
- reservation counters and latency timer
- health and metrics endpoints
- durable outbox status fields
- channel sync backlog gauges
- reconciliation run success and failure counters plus reconciliation duration timer
- in-app ops alerts for backlog, drift, stale snapshot, and scheduled reconciliation failure
- generic webhook-based external alert delivery with persisted delivery state
- ops copilot analysis success/failure counters plus duration timer
- promoted benchmark evidence cataloging through `testing/k6/evidence/index.json`

Current gap:

- vendor-specific observability integrations and richer alert-routing policy still need expansion beyond the generic webhook path
- benchmark evidence now has automated promotion and comparison output, but target coverage is still bounded to the current scenario set and promoted baselines

## Performance Targets

Target benchmark goals:

- handle `1000 orders/second`
- keep average latency under `200ms`
- demonstrate `0% overselling` under load

Interpretation:

- these are benchmark and validation targets
- they are not unconditional hard SLAs for every environment at the current maturity level

## Reliability Expectations

- reservation expiry must return stock safely
- failed publish attempts must not corrupt business state
- invalid transitions and conflicting updates must fail explicitly rather than silently

## Scalability Expectations

Current state:

- modular monolith
- MySQL + Redis + Kafka
- K6 smoke tests

Target direction:

- only introduce heavier scale mechanics such as partitioning, replication, or service decomposition when benchmark evidence justifies them
- benchmark evidence must come from promoted curated runs under `testing/k6/evidence/`
- the current informational baseline target is `testing/k6/evidence/20260315-133859-e2e3644/report.json`

## Verification Expectations

- backend changes should prefer `.\mvnw test`
- cross-boundary behavior should use integration tests
- admin/operator browser workflows should use Playwright in addition to page-level Vitest coverage
- focused web-slice or provider tests are acceptable for advisory AI and read-only admin additions when full Docker-backed integration proof is not available in the current workspace
- performance claims should be backed by repeatable K6 scenarios, not narrative only
