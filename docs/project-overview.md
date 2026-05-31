# Project Overview

**Last Updated:** 2026-05-30

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

Implemented capabilities:

- Centralized inventory as the source of truth
- Flash sale campaign windows and quota enforcement
- Soft reservation with expiry and release
- Order status progression from `PENDING` to `PAID` to `SHIPPED`
- Redis-backed SKU locking plus optimistic locking for inventory
- Outbox recording, retry scheduling, and scheduled Kafka publish
- Backward-compatible idempotency persistence for release and order status mutations
- Channel sync attempts and persisted channel inventory snapshots
- Shopee sandbox-ready real outbound sync and live inbound reconciliation reads
- Operator remediation APIs for outbox retry and reconciliation drift management
- Scheduled reconciliation runs inside the monolith
- In-app operational alert evaluation for backlog, drift, stale snapshots, and scheduled-run failure
- Generic webhook-based external alert delivery with persisted delivery state
- Provider-aware alert routing with generic webhook fallback plus first-class Slack and PagerDuty targets
- App-managed admin authentication with JWT access and refresh tokens
- Browser-safe refresh-cookie mode for admin UI callers while retaining the JSON refresh-token flow
- Role-gated admin APIs for campaign lifecycle management and audited ops remediation
- Benchmark evidence summary APIs over promoted K6 artifacts
- TikTok Shop real outbound sync, live inbound reconciliation reads, signed ingress APIs, and idempotent ingress receipts
- Dedicated admin channel-health summary API for marketplace posture
- Per-channel channel-health drill-down API for operator investigation
- Advisory-only Gemini-backed ops copilot APIs for operational analysis
- Admin/operator React SPA with campaigns, ops, benchmark, and channel-health workflows under `apps/admin-ui`
- Ops copilot panel embedded into the ops workflow in `apps/admin-ui`
- Playwright browser coverage for the key admin/operator workflows
- API and admin UI container packaging plus a simple-cloud CI baseline
- Versioned outbox event envelopes plus contract fixtures and simulator harnesses under `testing/contracts`
- Promoted K6 benchmark baseline under `testing/k6/evidence/20260315-133859-e2e3644/`
- Automated benchmark promotion, comparison output, and evidence cataloging under `testing/k6/evidence/index.json`

## Target Requirement Summary

Idea 02 targets an omnichannel inventory and flash sale engine that:

- preserves inventory correctness across `WEB`, `APP`, and marketplace channels
- tolerates flash sale traffic spikes without overselling
- supports 10-minute soft reservation
- synchronizes order status across connected systems
- provides benchmark evidence toward `1000 orders/sec` and `<200ms` average latency

## Implemented Capabilities

Implemented capabilities:

- Reservation create, confirm, release, and expire flows
- Flash sale window and quota checks
- Explicit reservation, order, and campaign states
- Durable outbox rows for business events
- Scheduled reservation expiry, outbox publish, and channel sync publish
- Scheduled reconciliation evidence capture without auto-correction
- Persisted release and order-status idempotency replay
- Channel snapshot reconciliation runs and drift resolution APIs
- Admin login, refresh, and logout flows with seeded local admin and operator identities
- HttpOnly refresh-cookie flow for browser admin clients
- Admin campaign create, update, activate, end, and campaign audit reads
- Admin ops wrappers for alerts, outbox retry, and reconciliation actions
- Admin channel-health read for marketplace posture
- Admin channel-health drill-down for per-channel failure context
- Admin benchmark evidence list/detail/latest reads
- Advisory ops copilot capabilities and analysis reads
- Ops alert surface for operational breaches and scheduler failure visibility
- TikTok inventory ingress and order-status ingress with signed callback verification
- Provider-aware alert delivery via webhook, Slack, and PagerDuty publishers
- React admin UI workflows for campaigns, ops, benchmarks, and channel health
- Error responses with correlation ID support

See:

- `docs/system-map.md`
- `docs/retrieval-guide.md`
- `docs/core-business-flows.md`

## Major Known Gaps

The current repository does not yet fully implement the target omnichannel vision:

- `WEB` and `APP` remain local/persisted channels, so omnichannel semantics are broader but still not fully externalized on every channel
- load testing has promoted baseline evidence and benchmark evidence APIs, but this workspace could not refresh release-grade evidence because Docker-backed services were unavailable
- simple-cloud packaging is now present, but runtime deployment proof on an actual target is still outstanding
- no read replicas, order partitioning, or service decomposition are implemented
- final release-readiness still depends on Docker-backed backend integration verification and full benchmark execution on a machine with working containers

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
