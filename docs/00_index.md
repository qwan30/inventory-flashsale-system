# Documentation Index

## Purpose

This `docs/` tree stores durable project knowledge for the inventory and flash sale system. Use it for information that should survive across sessions, handoffs, and implementation cycles.

Read in this order for broad project work:

1. `docs/AGENTS.md`
2. `docs/00_index.md`
3. `docs/project-overview.md`
4. `docs/system-map.md`
5. `docs/retrieval-guide.md`
6. `docs/actors.md`
7. `docs/system-modules.md`
8. `docs/core-business-flows.md`
9. `docs/business-rules.md`
10. `docs/data-model.md`
11. `docs/state-machine.md`
12. `docs/api-contract.md`
13. `docs/non-functional-requirements.md`
14. `docs/configuration-rules.md`
15. `docs/automation-tasks.md`
16. `docs/ui-roles.md`
17. The newest relevant file in `docs/03_implementation/`
18. The newest relevant file in `docs/05_history/`

## Structure

- `docs/system-map.md`
  Focused architecture map, main flows, invariants, persistence, and validated behavior.
- `docs/retrieval-guide.md`
  Fast-start guide for where to read first for bugs, features, schema work, or load issues.
- `docs/project-overview.md`
  High-level project statement, current repo summary, target requirement summary, and major gaps.
- `docs/actors.md`
  Business, system, and operational actors that interact with the platform.
- `docs/system-modules.md`
  Ownership and responsibility map for each module in the modular monolith.
- `docs/core-business-flows.md`
  Current and target business flows for reservation, order, expiry, and eventing.
- `docs/business-rules.md`
  Core domain rules and invariants that must hold during implementation changes.
- `docs/data-model.md`
  Current tables, entities, relationships, constraints, and target-only data gaps.
- `docs/state-machine.md`
  Reservation, campaign, order, and outbox states with their allowed transitions.
- `docs/api-contract.md`
  Current public API surface, headers, DTOs, error envelope, and API gaps.
- `docs/non-functional-requirements.md`
  Correctness, consistency, idempotency, observability, and benchmark targets.
- `docs/configuration-rules.md`
  Runtime dependencies, defaults, environment assumptions, and tuning guardrails.
- `docs/automation-tasks.md`
  Current schedulers and future operational automations, clearly labeled by implementation status.
- `docs/ui-roles.md`
  Business and operational roles in this backend-first system.
- `docs/01_ideation/`
  Problem framing, alternatives, constraints, and early design thinking.
- `docs/02_planning/`
  Roadmaps, milestone plans, and execution-ready plans.
- `docs/03_implementation/`
  Implementation notes, rollout notes, migration notes, and significant delivery records.
- `docs/03_implementation/2026-03-15-phase-1-hardening-slice.md`
  Focused Phase 1 hardening delivery for outbox retry, metrics, and reservation edge cases.
- `docs/03_implementation/2026-03-15-wave-1-3-monolith-foundation.md`
  Broader monolith-first delivery covering persisted idempotency, channel sync, ops remediation, and reconciliation foundation.
- `docs/03_implementation/2026-03-15-ops-closure-slice.md`
  Operational closure for scheduled reconciliation, alert evaluation, and repeatable benchmark artifact generation.
- `docs/04_audit_remediation/`
  Risk audits, bug investigations, remediation plans, and postmortems.
- `docs/05_history/`
  Concise dated records of important completed work or important discoveries worth reloading next session.

## What Belongs Here

- Decisions that affect future implementation.
- System knowledge that would otherwise need to be rediscovered.
- Completed milestones worth remembering.
- Audits and follow-up items that should stay visible.

## What Does Not Belong Here

- Raw chat transcripts.
- Scratch notes with no lasting value.
- Repeated copies of source code.
- AI workspace notes for `skills/` or `everything-claude-code/`.

## History Policy

`docs/05_history/` is for durable signal, not exhaustive logs.

Add a history entry when:

- a meaningful discovery pass changes how future work should start
- a milestone ships
- a bug or risk investigation produces lasting conclusions
- a migration, rollback, or remediation changes operational assumptions

Keep each entry short and include:

- date
- what changed or was learned
- evidence
- what future sessions should do with that knowledge
