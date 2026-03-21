# 2026-03-16 Admin UI Screen Spec And Wireframe Plan

## Summary

This doc is the lightweight screen spec for the in-repo admin UI under `apps/admin-ui`. It replaces the need for a full Figma-first pass for the current internal operator surface.

The intent is:

- keep the first UI shell implementation aligned with the backend that already exists
- give future frontend work a stable screen map and panel breakdown
- provide a wireframe-level plan for `campaigns`, `ops`, and `benchmarks` without over-specifying visual polish
- extend the shell with the next high-value operational screens before any polished design pass

## App Frame

### Routes

- `/login`
- `/campaigns`
- `/ops`
- `/benchmarks`

### Shared Shell

- left sidebar with product label, app title, nav items, user identity, and sign-out
- main content area with one page header and stacked content sections
- route guard redirects unauthenticated users to `/login`
- browser clients use access token in memory and backend-managed refresh cookie

### Shared States

- loading: inline text or card-level loading state, never blank screens
- empty: one short sentence describing why the list or panel is empty
- error: inline error banner in the affected panel, not a global crash for panel-level fetch failures
- expired session: redirect to login after refresh failure

## Screen Spec

### Login

Purpose:

- start a browser-safe admin session

Structure:

- centered auth card
- username field
- password field
- submit CTA
- inline error state

Primary success path:

- `POST /api/v1/admin/auth/login`
- persist access token in memory only
- rely on HttpOnly refresh cookie when enabled

### Campaigns

Purpose:

- give admins a quick operational view of campaign inventory and lifecycle readiness

Structure:

- page header with title and context label
- three summary cards:
  - active campaigns
  - draft campaigns
  - total quota
- one main ledger panel

Ledger columns:

- campaign id
- sku
- status
- quota
- reserved quota
- sold quota

Required data:

- `GET /api/v1/admin/campaigns`

Next UI actions to add later:

- create draft
- edit draft
- activate
- end
- audit detail drawer or route

### Campaign Detail / Edit

Purpose:

- let admins inspect one campaign and perform lifecycle-safe actions without leaving the operator shell

Structure:

- header with campaign id, sku, status badge
- summary strip:
  - starts at
  - ends at
  - quota
  - reserved quota
  - sold quota
- editable draft form section
- lifecycle action section

Editable fields:

- starts at
- ends at
- quota

Actions:

- save draft update
- activate campaign
- end campaign
- open audit history

Required data:

- existing campaign list payload can seed the view initially
- follow-on detail endpoint may be added later if list payload becomes too shallow

### Campaign Audit

Purpose:

- show immutable admin activity around one campaign so operators can understand who changed what and when

Structure:

- campaign context header
- audit timeline or audit table

Required fields:

- action
- actor username
- actor role
- outcome
- correlation id
- timestamp
- detail text

Required data:

- `GET /api/v1/admin/campaigns/{campaignId}/audits`

### Ops

Purpose:

- help operators spot backlog, drift, and alert conditions quickly

Structure:

- page header
- top row with two panels:
  - outbox backlog metrics
  - open drifts summary
- full-width alert matrix panel

Outbox panel metrics:

- pending
- failed
- retryable failed

Drift list fields:

- channel
- sku
- status

Alert matrix fields:

- alert code
- message
- severity
- status
- current value
- threshold

Required data:

- `GET /api/v1/admin/ops/outbox/backlog`
- `GET /api/v1/admin/ops/reconciliation/drifts`
- `GET /api/v1/admin/ops/alerts`

Next UI actions to add later:

- retry outbox event
- trigger reconciliation run
- resolve drift
- alert filtering by severity or state

### Ops Remediation

Purpose:

- move from passive monitoring into operator actions for outbox and reconciliation failures

Structure:

- tab or segmented layout:
  - outbox failures
  - reconciliation runs
  - drift detail
- action drawer or inline action row for remediation

Outbox failures section:

- failed event id
- status
- attempts
- last error
- retry action

Reconciliation runs section:

- run id
- trigger type
- status
- scanned count
- open drift count
- started or completed time

Drift detail section:

- channel
- sku
- central snapshot
- observed snapshot
- resolution note input
- resolve action

### Benchmarks

Purpose:

- surface promoted benchmark evidence without forcing operators to browse files manually

Structure:

- page header
- two-column layout:
  - left panel: promoted run list
  - right panel: selected run detail

Run list fields:

- run id
- suite status
- git commit

Run detail fields:

- suite status
- business checks passed
- git commit
- raw report view

Required data:

- `GET /api/v1/admin/ops/benchmarks/evidence`
- `GET /api/v1/admin/ops/benchmarks/evidence/{runId}`
- `GET /api/v1/admin/ops/benchmarks/evidence/latest`

Next UI actions to add later:

- comparison delta summary cards
- scenario-level charts
- link-out to promoted manifest/report files

### Benchmark Run Detail / Compare

Purpose:

- turn one promoted benchmark run into an operator-readable decision surface

Structure:

- selected run header
- key metrics cards
- scenario summary table
- baseline comparison block
- raw manifest and report inspectors

Key metric cards:

- suite status
- business checks passed
- baseline target
- git commit

Scenario summary fields:

- scenario name
- status
- average latency
- p95 latency
- failed rate
- checks rate

Comparison block:

- current vs baseline note
- per-scenario deltas when comparison data exists

## Additional Screens

### Channel Health

Purpose:

- give one place to inspect marketplace connector posture across Shopee and TikTok before debugging lower-level logs

Structure:

- channel selector or stacked channel cards
- sync health summary
- live vs persisted snapshot indicators
- ingress receipts or replay activity list
- connector mode and config state section

Recommended blocks:

- sync backlog by channel
- stale snapshot count by channel
- most recent ingress receipts
- most recent replay actions
- connector mode badges:
  - `WEB` local
  - `APP` local
  - `SHOPEE` mock or real
  - `TIKTOK_SHOP` mock or real

Required data:

- existing ops alert and reconciliation data for high-level health
- TikTok replay and ingress receipt surfaces as they mature
- future channel-specific summary endpoint if this view becomes too composite

## Wireframe Plan

### Navigation Model

- login is standalone and unauthenticated
- all authenticated screens live inside one persistent shell
- sidebar order stays:
  - campaigns
  - ops
  - benchmarks
- second-level routes or drawers should be added for:
  - campaign detail/edit
  - campaign audit
  - ops remediation
  - benchmark run detail
  - channel health

### Desktop Layout

- sidebar fixed at left
- content area uses one-column stacking for page sections
- pages may use nested two-column panels only inside content, not nested app chrome

### Mobile/Small Width

- sidebar should collapse into a top nav or drawer
- page panels stack vertically
- tables degrade into card rows instead of dense grids

### Component Reuse

- shell layout
- page header
- panel container
- metric card
- inline error banner
- loading and empty placeholders
- drawer or detail panel
- timeline or activity table
- key-value snapshot comparison block

## Assumptions

- this is an internal operator UI, not a public customer product
- backend APIs remain the source of truth for actions and validation
- current UI shell remains intentionally simple until benchmark reporting and ops workflows settle further
- deeper visual polish can follow after the operator workflow itself stabilizes
- the next route expansion should favor `Campaign Detail / Edit`, `Ops Remediation`, `Benchmark Run Detail / Compare`, and `Channel Health` before broader cosmetic work
