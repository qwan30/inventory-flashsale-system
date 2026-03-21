# Admin Backend Closure Plan

## Summary

**Task class:** `standard feature`

Finish the backend side of the remaining admin workflows first and defer all new frontend execution. The goal of this slice is to make the admin backend contract-complete for the missing `Campaign Detail / Audit`, `Ops Remediation`, and `Benchmark Run Detail / Compare` flows so a later UI pass can bind to stable APIs without backend guesswork.

Frontend routes and screens stay out of scope for this slice.

## Key Changes

### Campaign management contracts

Add a dedicated detail read:

- `GET /api/v1/admin/campaigns/{campaignId}`

Behavior:

- return the existing `AdminCampaignResponse` shape for one campaign
- use the same source of truth as `listCampaigns()`
- return `404` when the campaign does not exist
- keep existing create, update, activate, end, and audit endpoints unchanged

### Ops remediation read models

Add a failed-outbox listing endpoint:

- `GET /api/v1/admin/ops/outbox/events`

Default contract:

- default filter is failed events only
- optional query params:
  - `status`, default `FAILED`
  - `limit`, default `50`, max `200`
- order newest operationally relevant items first using audit timestamps

Add a reconciliation-run history endpoint:

- `GET /api/v1/admin/ops/reconciliation/runs`

Default contract:

- optional `limit`, default `20`, max `100`
- order newest runs first
- response fields include existing `ReconciliationRunResponse` fields plus `createdAt`

Keep existing action endpoints unchanged:

- `POST /api/v1/admin/ops/outbox/{eventId}/retry`
- `POST /api/v1/admin/ops/reconciliation/runs`
- `POST /api/v1/admin/ops/reconciliation/{driftId}/resolve`

### Benchmark evidence normalization

Keep the existing evidence endpoints, but enrich the detail contract so the future benchmark screen does not parse raw k6 JSON in the browser.

Extend `GET /api/v1/admin/ops/benchmarks/evidence/{runId}` and `/latest` to include typed summary data alongside the existing raw payloads:

- `suiteSummary`
- `scenarioSummaries[]`
- `scenarioComparisons[]`

Compatibility rule:

- preserve current `entry`, `manifest`, `report`, `comparison`, and `summaryMarkdown` fields so existing callers and docs do not break

## Verification

- backend integration coverage for campaign detail, ops read models, and benchmark summaries
- `.\mvnw test`

## Assumptions

- frontend execution is fully deferred in this slice
- `Channel Health` remains out of scope
- no Flyway migration is needed; required timestamps already exist via `AuditTimestamps`
- campaign audit remains sufficient as-is; only campaign detail read is added
- benchmark normalization is derived from the current evidence files without changing the k6 artifact format
