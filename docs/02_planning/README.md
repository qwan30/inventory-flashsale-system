# Pre-Implementation Documentation Set Plan

**Last Updated:** 2026-03-15

## Purpose

Create a durable top-level documentation set under `docs/` before implementation work continues. The new docs must describe:

- the current repository as it exists today
- the target requirement direction from Idea 02
- the gap between current implementation and target capability

This milestone is documentation-only. It must not change application behavior, schema, or API contracts.

## Why This Documentation Set Exists

- The repository already implements a substantial slice of the omnichannel inventory and flash sale idea, so planning and implementation need a shared view of current state versus target gaps.
- Future implementation tasks should not need to rediscover actors, flows, state transitions, API contracts, business rules, or configuration assumptions.
- The new docs should become durable project memory for both humans and agents, while `docs/system-map.md` and `docs/retrieval-guide.md` remain the low-level anchor docs.

## Authoring Order

Write the docs in this order so later files can reference earlier ones:

1. `docs/project-overview.md`
2. `docs/actors.md`
3. `docs/system-modules.md`
4. `docs/core-business-flows.md`
5. `docs/business-rules.md`
6. `docs/data-model.md`
7. `docs/state-machine.md`
8. `docs/api-contract.md`
9. `docs/non-functional-requirements.md`
10. `docs/configuration-rules.md`
11. `docs/automation-tasks.md`
12. `docs/ui-roles.md`
13. Update `docs/00_index.md`
14. Update `docs/AGENTS.md` if the new docs materially improve future prompt routing
15. Add a history entry if the documentation pass creates durable project memory

## Source Of Truth Inputs

Use the existing repository as the primary source of truth. Derive current-state documentation from:

- `docs/system-map.md`
- `docs/retrieval-guide.md`
- controllers and API DTOs in `apps/api`
- enums, entities, and services in `modules/`
- Flyway migrations in `apps/api/src/main/resources/db/migration`
- `application.yml`
- schedulers and configuration properties
- integration and module tests

Do not describe target features as implemented unless the code already exists.

## Required Output Characteristics

- Every new doc must explicitly separate:
  - current implemented behavior
  - target requirement direction
  - current gaps or deferred capabilities
- The writing style should stay factual, concise, and implementation-oriented.
- New docs should link to `docs/system-map.md` and `docs/retrieval-guide.md` instead of duplicating all source-level detail.
- `UI role` must mean business and operational roles, not frontend screen specs.
- Future or planned items must be labeled as planned, target, or not yet implemented.

## Acceptance Checklist

- `docs/project-overview.md` summarizes the problem, current repo, target direction, implemented capabilities, major gaps, and links to the rest of the docs.
- `docs/actors.md` covers shopper, sales channels, internal services, schedulers, operator/admin roles, and downstream consumers.
- `docs/system-modules.md` maps ownership for `common`, `channel`, `flashsale`, `inventory`, `order`, and `outbox`, including current responsibilities and planned extensions.
- `docs/core-business-flows.md` covers reserve, confirm, release, expire, order status sync, outbox publish, and target omnichannel sync or reconciliation flow.
- `docs/business-rules.md` captures current rules for inventory correctness, flash sale windows and quotas, idempotency, conflict conditions, release and expiry, and order transitions.
- `docs/data-model.md` matches the current tables, entities, important fields, and constraints, and clearly labels any target-only additions as not implemented.
- `docs/state-machine.md` matches the current reservation, campaign, order, and outbox states and transitions.
- `docs/api-contract.md` matches the current HTTP routes, headers, DTOs, success shapes, and error envelope.
- `docs/non-functional-requirements.md` captures correctness, consistency, idempotency, observability, and benchmark-oriented performance goals.
- `docs/configuration-rules.md` captures defaults, environment dependencies, scheduler timings, and operational configuration guidance.
- `docs/automation-tasks.md` covers only real current automated tasks plus clearly labeled future operational automations.
- `docs/ui-roles.md` documents business-role responsibilities in a backend-first system.
- `docs/00_index.md` lists and links the new docs in a useful reading order.
- `docs/AGENTS.md` points future sessions to the new docs when they are the right source.
- A `docs/05_history/` entry records the creation of this documentation set because it is a durable documentation milestone.

## Assumptions

- The repository remains a modular monolith unless later implementation work proves otherwise.
- Heavy scale changes such as partitioning, replication, or service decomposition should be documented as target gaps unless the code already supports them.
- The `1000 orders/sec` and `<200ms average latency` figures are benchmark targets, not immediate hard guarantees for all environments.
