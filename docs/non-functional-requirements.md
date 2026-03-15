# Non-Functional Requirements

**Last Updated:** 2026-03-15

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

Current gap:

- richer benchmark evidence storage and external observability still need expansion
- benchmark evidence promotion workflow must be completed so decisions can depend on durable baselines, not transient run output

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

## Verification Expectations

- backend changes should prefer `.\mvnw test`
- cross-boundary behavior should use integration tests
- performance claims should be backed by repeatable K6 scenarios, not narrative only
