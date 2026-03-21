# 2026-03-16 Idea 02 Progress Checklist

## Scope

Compared the current repository against the target in `docs/01_ideation/idea-02-omnichannel-inventory-flash-sale-engine.md` plus the current canonical summaries in `docs/project-overview.md`, `docs/system-map.md`, `docs/core-business-flows.md`, `docs/api-contract.md`, and `docs/non-functional-requirements.md`.

This is a status audit, not a new implementation plan.

## Evidence Reviewed

- target requirement statement:
  - `docs/01_ideation/idea-02-omnichannel-inventory-flash-sale-engine.md`
  - `docs/project-overview.md`
- current architecture and flow docs:
  - `docs/system-map.md`
  - `docs/core-business-flows.md`
  - `docs/api-contract.md`
  - `docs/non-functional-requirements.md`
- recent delivery records:
  - `docs/03_implementation/2026-03-15-v1-completion-wave.md`
  - `docs/03_implementation/2026-03-16-production-hardening-channel-health-and-e2e.md`
- workspace verification run on 2026-03-16:
  - `.\mvnw.cmd -pl apps/api -am -DskipTests compile` -> passed
  - `npm test` in `apps/admin-ui` -> passed
  - `npm run build` in `apps/admin-ui` -> passed
  - `npm run test:e2e` in `apps/admin-ui` -> passed
  - `.\mvnw.cmd test` -> failed at API integration tests because Testcontainers could not find a valid Docker environment

## Requirement Checklist

### 1. Centralized Inventory Correctness

Status: `implemented`

Evidence:

- central inventory remains the source of truth in the current architecture docs
- reserve, confirm, release, and expire flows are implemented and documented end to end
- Redis SKU locking plus optimistic locking remain part of the baseline
- unit and module tests still run successfully in the current workspace

Assessment:

- this is one of the strongest completed areas in the repo

### 2. Flash Sale Mechanism

Status: `implemented`

Evidence:

- campaign lifecycle exists with `DRAFT`, `ACTIVE`, and `ENDED`
- campaign window and quota checks are part of the reservation path
- admin APIs and admin UI workflows exist for campaign management and audit history

Assessment:

- the repository now clearly exceeds the original ideation-level flash sale requirement

### 3. Soft Reservation For 10 Minutes

Status: `implemented`

Evidence:

- reservation TTL defaults to `10m`
- reservation expiry sweep is implemented
- expiry returns stock and reserved campaign quota safely

Assessment:

- this requirement is functionally complete in the current baseline

### 4. Order Status Synchronization Across Connected Systems

Status: `partially implemented`

Evidence:

- the central order lifecycle `PENDING -> PAID -> SHIPPED` is implemented
- order status mutation records outbox events
- TikTok signed ingress can normalize external order-status updates into the central flow

Open gap:

- the broader omnichannel target is not complete yet because inbound marketplace order flows are still limited and not every external channel has equivalent real inbound order synchronization

### 5. Omnichannel Inventory Synchronization

Status: `partially implemented`

Evidence:

- `WEB` and `APP` are supported as internal channels
- Shopee has real outbound sync and live inbound reconciliation reads
- TikTok Shop has real outbound sync, signed ingress, idempotent receipts, and channel-health visibility
- reconciliation, drift management, and channel snapshots are implemented

Open gap:

- `WEB` and `APP` are still local/persisted channels rather than full external integrations
- omnichannel semantics are real, but not fully externalized across every channel in the target vision
- richer drift analytics and auto-remediation are still open

### 6. Admin And Operator Product Surface

Status: `implemented for current V1 scope`

Evidence:

- admin auth, campaign management, ops remediation, benchmark evidence views, and channel health all exist
- the admin React app has page-level Vitest coverage and Playwright workflow coverage
- current browser verification passed in this workspace

Assessment:

- this area is ahead of the original idea doc and should not be treated as a major remaining gap

### 7. Performance Evidence Toward `1000 orders/sec` And `<200ms`

Status: `partially implemented`

Evidence:

- a promoted K6 evidence baseline exists under `testing/k6/evidence/20260315-133859-e2e3644/`
- current promoted scenario averages are below `200ms`
- concurrency and stock-conservation evidence support the current correctness claim for the covered scenarios

Open gap:

- there is still no evidence-backed claim that the system sustains `1000 orders/sec`
- the hottest scenario has tail latency that is still uncomfortable
- this workspace could not refresh release-grade benchmark evidence because Docker-backed services were unavailable

### 8. Release-Readiness Proof

Status: `partially implemented / environment-blocked`

Evidence:

- Java compile passed
- frontend test, build, and Playwright passed
- `.\mvnw.cmd test` currently fails only when API integration tests try to start Testcontainers without Docker

Open gap:

- full backend integration proof is still missing on this machine
- benchmark refresh remains blocked until Docker-backed dependencies can run

## Percentage Estimate

Two numbers are useful here because raw feature count and evidence-backed readiness are not the same thing.

- feature completeness against the current V1 scope: `about 85%`
- requirement closure against the original Idea 02 target including proof obligations: `about 78%`

Reasoning:

- the core inventory, flash sale, reservation, admin, and operator capabilities are largely in place
- the remaining gap is concentrated in omnichannel depth and proof, not in the basic product skeleton
- the biggest unfinished items are evidence and externalization, not the reservation engine itself

## Practical Conclusion

If someone asks "is the system mostly built already?", the answer is yes.

If someone asks "is the full target requirement completely proven and release-ready?", the answer is no yet.

The shortest accurate summary is:

- core product and operations scope: mostly done
- omnichannel depth: partially done
- benchmark and release proof: not done enough yet

## Next Work That Moves The Percentage

The next highest-value moves are:

1. restore Docker-backed backend verification so `.\mvnw test` can complete
2. rerun and promote a fresh K6 evidence set on a Docker-capable machine
3. close the remaining omnichannel depth gaps only after the evidence path is healthy again
