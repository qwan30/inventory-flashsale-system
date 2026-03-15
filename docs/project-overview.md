# Project Overview

**Last Updated:** 2026-03-15

## Problem Statement

This project addresses high-risk inventory problems in an omnichannel commerce system:

- overselling caused by concurrent purchase attempts
- inconsistent stock views across sales channels
- flash sale bursts that stress the reservation and checkout path
- delayed order and fulfillment updates across connected systems

The system aims to keep one central inventory truth while supporting time-boxed flash sales, soft reservation, and event-driven downstream integration.

## Current Repository Summary

Current codebase shape:

- Java 21 + Spring Boot 3 modular monolith
- deployable app in `apps/api`
- bounded modules in `modules/common`, `channel`, `flashsale`, `inventory`, `order`, and `outbox`
- MySQL for durable state
- Redis for distributed lock coordination
- Kafka for outbox-driven event publication
- Testcontainers-backed integration tests
- K6 smoke scripts under `testing/k6`

The current repository already implements:

- centralized inventory as the source of truth
- flash sale campaign windows and quota enforcement
- soft reservation with expiry and release
- order status progression from `PENDING` to `PAID` to `SHIPPED`
- Redis-backed SKU locking plus optimistic locking for inventory
- outbox recording, retry scheduling, and scheduled Kafka publish
- backward-compatible idempotency persistence for release and order status mutations
- channel sync attempts and persisted channel inventory snapshots
- Shopee sandbox-ready real outbound sync and live inbound reconciliation reads
- operator remediation APIs for outbox retry and reconciliation drift management
- scheduled reconciliation runs inside the monolith
- in-app operational alert evaluation for backlog, drift, stale snapshots, and scheduled-run failure
- generic webhook-based external alert delivery with persisted delivery state
- one promoted K6 benchmark baseline under `testing/k6/evidence/20260315-133859-e2e3644/`
- automated benchmark promotion, comparison output, and evidence cataloging under `testing/k6/evidence/index.json`

## Target Requirement Summary

Idea 02 targets an omnichannel inventory and flash sale engine that:

- preserves inventory correctness across `WEB`, `APP`, and marketplace channels
- tolerates flash sale traffic spikes without overselling
- supports 10-minute soft reservation
- synchronizes order status across connected systems
- provides benchmark evidence toward `1000 orders/sec` and `<200ms` average latency

## Implemented Capabilities

Current implemented capabilities include:

- reservation create, confirm, release, and expire flows
- flash sale window and quota checks
- explicit reservation, order, and campaign states
- durable outbox rows for business events
- scheduled reservation expiry, outbox publish, and channel sync publish
- scheduled reconciliation evidence capture without auto-correction
- persisted release and order-status idempotency replay
- channel snapshot reconciliation runs and drift resolution APIs
- ops alert surface for operational breaches and scheduler failure visibility
- error responses with correlation ID support

See:

- `docs/system-map.md`
- `docs/retrieval-guide.md`
- `docs/core-business-flows.md`

## Major Known Gaps

The current repository does not yet fully implement the target omnichannel vision:

- only Shopee has a real sandbox connector path; `WEB` and `APP` remain local/persisted channels and no second marketplace connector exists yet
- alerting now has a generic webhook delivery path, but no richer vendor-specific observability stack or routing policy exists yet
- load testing has a promoted checked-in baseline and automated evidence workflow, but the current evidence program still covers only the existing benchmark scenarios
- no read replicas, order partitioning, or service decomposition are implemented
- no real admin or operator-facing UI exists in this repository

## Recommended Reading Order

1. `docs/system-map.md`
2. `docs/retrieval-guide.md`
3. `docs/actors.md`
4. `docs/system-modules.md`
5. `docs/core-business-flows.md`
6. `docs/business-rules.md`
7. `docs/data-model.md`
8. `docs/state-machine.md`
9. `docs/api-contract.md`
10. `docs/non-functional-requirements.md`
11. `docs/configuration-rules.md`
12. `docs/automation-tasks.md`
13. `docs/ui-roles.md`
