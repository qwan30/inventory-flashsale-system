# UI Roles

**Last Updated:** 2026-03-16

## Interpretation

This repository is backend-first. `UI role` in this documentation set means business and operational roles that would interact with the system, not implemented screens or frontend wireframes.

## Shopper Role

Current responsibilities:

- browse inventory availability indirectly
- create a reservation during flash sale
- confirm purchase

Current interaction surface:

- public reservation and inventory APIs

## Marketplace / Channel Role

Current responsibilities:

- identify the source channel for a reservation or order flow

Current gap:

- no real external UI or marketplace console integration is implemented in this repository
- channel behavior is represented as API-level origin and adapter logic only

## Operator Role

Current responsibilities:

- monitor inventory correctness, lock contention, scheduler behavior, and outbox backlog
- run tests, smoke checks, and benchmark scripts
- investigate failures or drift

Current likely tools:

- logs
- metrics endpoints
- Kafka and infrastructure tooling
- manual test or benchmark commands
- the operator routes in `apps/admin-ui` for ops, benchmarks, and channel health

## Future Admin Role

Target future responsibilities:

- campaign management
- reconciliation review
- outbox failure remediation
- benchmark and operational reporting

Current implemented surface:

- secured admin and operator APIs are implemented
- a React admin/operator UI now exists in `apps/admin-ui`
- admins use campaign workflows
- admins and operators use ops, benchmarks, and channel-health workflows
- browser-safe refresh-cookie auth mode is available for the UI
- Playwright browser coverage now exists for the key admin/operator workflows

Current gap:

- the UI is now workflow-capable, but final release readiness still depends on Docker-backed backend verification and refreshed benchmark evidence

## Documentation Boundary

- this file is not a screen specification
- if a future frontend is built, that UI design should live in separate frontend or product docs
