# 2026-03-16 Admin UI Closure Wave

## Scope Delivered

This slice closes the admin UI workflow gaps that were still open after the initial shell and the backend-first contract pass.

Delivered in code under `apps/admin-ui`:

- route-first campaign workflows:
  - `/campaigns/:campaignId`
  - `/campaigns/:campaignId/audits`
- operator remediation route:
  - `/ops/remediation`
- benchmark detail route:
  - `/benchmarks/:runId`
- protected-route session bootstrap from the refresh-cookie flow so reloads and deep links do not drop straight to login
- role-aware frontend routing:
  - campaign routes stay `ADMIN` only
  - ops and benchmarks stay available to `ADMIN` and `OPERATOR`
- feature-folder reorganization for `campaigns`, `ops`, and `benchmarks`
- shared frontend utilities for:
  - typed admin API reads and mutations
  - async action state
  - inline loading, empty, error, and success banners
  - datetime, number, percent, and delta formatting
  - reusable status badges

## Behavior Added

### Campaigns

- campaign ledger rows now launch directly into campaign detail
- campaign detail binds to the dedicated admin campaign read contract and surfaces:
  - state-aware lifecycle actions
  - editable draft fields
  - read-only reserved and sold quota facts
- campaign audit binds to the audit read contract and derives:
  - total actions
  - failed attempts
  - unique actors
- ended campaigns are read-only except for audit access

### Ops

- operator redirects from forbidden campaign routes now land on ops with an inline notice
- ops overview links into remediation only; channel health remains deferred for this slice
- ops remediation ships tabbed operator actions for:
  - failed outbox events with inline retry
  - reconciliation run history plus manual run trigger
  - drift inspection with inline resolve note and resolve action

### Benchmarks

- benchmark overview remains the promoted run list, but now navigates into a dedicated run detail route
- benchmark detail renders typed benchmark evidence summaries and scenario comparisons instead of depending on raw JSON browsing alone
- benchmark detail gracefully handles the current `no baseline available` fixture state without inventing comparison deltas

## Security And Guardrails

- no secrets, browser storage tokens, or debug logging were added in this slice
- access token handling remains in memory; session restore continues to rely on the backend-managed refresh cookie
- campaign authorization is enforced twice:
  - frontend route and nav gating for operator ergonomics
  - existing backend authorization for actual enforcement on privileged actions
- no backend contracts changed in this slice; privileged mutations continue to use the already-landed admin endpoints and audit logging

## Verification

Executed:

- `npm test`
- `npm run build`

Observed result:

- all admin UI tests pass
- production build succeeds
- no dedicated lint script is configured in `apps/admin-ui`, so lint verification could not be run

## Notes For Future Sessions

- keep `Channel Health` out of the active admin route map until a dedicated backend summary endpoint exists
- richer channel posture UI should be added only after backend support for connector mode, ingress receipts, and replay history is available
- benchmark compare remains intentionally inside `/benchmarks/:runId`; no arbitrary-baseline route was added in this slice
- `apps/admin-ui/src/views/channels/**` still exists as an unwired experiment and should not be treated as shipped product surface
