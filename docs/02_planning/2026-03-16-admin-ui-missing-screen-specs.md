# 2026-03-16 Admin UI Missing Screen Specs

## Purpose

This note identifies the admin UI routes that are still missing or still need a first-class design pass beyond the current top-level screens. It is derived from `docs/02_planning/2026-03-16-admin-ui-screen-spec-and-wireframe-plan.md` and is intended to keep the next design iteration focused on workflow closure rather than adding more top-level dashboards.

## Scope Decision

The current top-level screen set is already sufficient for V1:

- `Login`
- `Campaigns`
- `Ops`
- `Benchmarks`

What is still missing is the supporting route set that turns those overview pages into complete operator workflows.

## Missing Screens

### 1. Campaign Detail / Edit

Status:

- required for V1
- should be the first missing screen to complete

Why it is needed:

- the campaign overview cannot remain list-only if admins need to inspect dates, quota, and lifecycle actions safely

Route shape:

- `campaigns/:campaignId`
- modal or drawer is possible later, but route-first is safer for V1

### Wireframe

- shell stays unchanged with the persistent left sidebar
- page header:
  - back link to campaigns
  - campaign id
  - sku label
  - status badge
- summary strip:
  - starts at
  - ends at
  - quota
  - reserved quota
  - sold quota
- main body:
  - left: editable campaign form
  - right: lifecycle rules or warning panel
- footer action row:
  - save draft changes
  - activate campaign
  - end campaign
  - open audit history

### Spec

Purpose:

- inspect one campaign and perform lifecycle-safe actions without leaving the admin shell

Required data:

- seed from `GET /api/v1/admin/campaigns`
- optional dedicated detail read later if the list payload becomes too shallow

Editable fields:

- starts at
- ends at
- quota

Rules:

- action set must be state-aware
- draft campaigns can be edited and activated
- active campaigns can be ended but should not show `activate`
- ended campaigns should be read-only except audit access
- show reserved and sold quota as read-only operational facts

### 2. Campaign Audit

Status:

- required for V1

Why it is needed:

- campaign management without immutable audit visibility is incomplete for an admin surface

Route shape:

- `campaigns/:campaignId/audits`

### Wireframe

- shell unchanged
- compact context header:
  - campaign id
  - sku
  - status
  - quick jump back to detail
- stat strip:
  - total actions
  - failed attempts
  - active actors
  - optional last sync
- main panel:
  - audit table as the primary surface
- optional secondary row:
  - retention policy
  - recent system errors or audit system notes

### Spec

Purpose:

- show immutable admin activity around one campaign so operators can understand who changed what and when

Required data:

- `GET /api/v1/admin/campaigns/{campaignId}/audits`

Required table fields:

- action
- actor username
- actor role
- outcome
- correlation id
- timestamp
- detail text or clear affordance to open row detail

Rules:

- audit table must remain the primary panel
- supplemental cards must stay secondary
- row detail can be inline expansion, drawer, or detail panel

### 3. Ops Remediation

Status:

- required for V1 if the ops page is expected to drive action, not just monitoring

Why it is needed:

- the current ops overview can detect failures, but operators still need a place to retry, resolve, and inspect remediation history

Route shape:

- `ops/remediation`

### Wireframe

- shell unchanged
- page header:
  - title
  - short operational context line
  - primary CTA for reconciliation run if enabled
- segmented control or tabs:
  - outbox failures
  - reconciliation runs
  - drift detail
- panel area changes by selected tab

Outbox failures tab:

- table with:
  - event id
  - status
  - attempts
  - last error
  - last updated
  - retry action

Reconciliation runs tab:

- table with:
  - run id
  - trigger type
  - status
  - scanned count
  - open drift count
  - started or completed time

Drift detail tab:

- comparison block:
  - channel
  - sku
  - central snapshot
  - observed snapshot
- remediation section:
  - resolution note
  - resolve action

### Spec

Purpose:

- move from passive monitoring into operator actions for outbox and reconciliation failures

Required data:

- current spec already depends on the ops family endpoints
- additional remediation endpoints can be layered in as the UI actions become live

Rules:

- destructive or corrective actions must be separated clearly from inspection data
- retry and resolve actions must show inline success or failure state
- each tab needs loading, empty, and panel-level error states

### 4. Benchmark Run Detail / Compare

Status:

- required for V1 if benchmarks are meant to support decisions instead of just run browsing

Why it is needed:

- the benchmarks overview shows promoted runs, but operators still need a focused comparison surface for one run versus baseline

Route shape:

- `benchmarks/:runId`
- optional compare state such as `benchmarks/:runId/compare/:baselineId`

### Wireframe

- shell unchanged
- selected run header:
  - run id
  - suite status
  - business checks
  - git commit
  - baseline target
- key metrics cards row
- scenario summary table
- comparison block:
  - current vs baseline note
  - per-scenario deltas
- bottom utilities:
  - raw manifest link
  - raw report link
  - promoted artifact links

### Spec

Purpose:

- turn one promoted benchmark run into an operator-readable decision surface

Required data:

- `GET /api/v1/admin/ops/benchmarks/evidence/{runId}`
- `GET /api/v1/admin/ops/benchmarks/evidence/latest`

Required scenario fields:

- scenario name
- status
- average latency
- p95 latency
- failed rate
- checks rate

Rules:

- regressions must be visually more prominent than improvements
- raw evidence links should be present but secondary
- this screen should answer `is this run better or worse than baseline` without forcing file browsing

### 5. Channel Health

Status:

- recommended, but optional for the first V1 screen cut

Why it is needed:

- once Shopee and TikTok operational issues increase, a composite channel posture screen becomes useful

Route shape:

- `channels/health`
- should stay a secondary route, not a required top-level nav item for the first cut

### Wireframe

- shell unchanged
- page header with channel posture summary
- stacked channel cards or selector
- panels:
  - sync backlog by channel
  - stale snapshot count
  - latest ingress receipts
  - latest replay actions
  - connector mode and config state

### Spec

Purpose:

- inspect marketplace connector posture across Shopee and TikTok before debugging lower-level logs

Required data:

- reuse high-level ops and reconciliation data at first
- add a channel summary endpoint later if the view becomes too composite

Rules:

- keep it secondary until real connector operations justify promoting it
- do not let it replace the main ops dashboard

## Recommended Build Order

1. `Campaign Detail / Edit`
2. `Campaign Audit`
3. `Ops Remediation`
4. `Benchmark Run Detail / Compare`
5. `Channel Health`

## Conclusion

The admin UI does not need more top-level dashboards. It needs the missing workflow-closing screens that let operators move from overview into inspection, decision, and remediation.
