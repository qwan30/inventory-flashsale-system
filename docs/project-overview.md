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
- outbox recording and scheduled Kafka publish

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
- scheduled reservation expiry and outbox publish
- error responses with correlation ID support

See:

- `docs/system-map.md`
- `docs/retrieval-guide.md`
- `docs/core-business-flows.md`

## Major Known Gaps

The current repository does not yet fully implement the target omnichannel vision:

- channel integrations are still mock adapters and request validators, not real channel sync boundaries
- there is no full reconciliation workflow for channel drift detection
- outbox failure handling exists, but retry and remediation semantics are still limited
- load testing is smoke-level, not yet a staged benchmark program
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
