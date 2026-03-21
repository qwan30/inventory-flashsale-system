# 2026-03-16 Production Hardening Channel Health And E2E

## Scope Delivered

This slice completed the production-hardening work that was still open after the admin backend and admin UI closure waves.

Delivered in code:

- dedicated marketplace posture API:
  - `GET /api/v1/admin/channels/health`
- backend channel-health read model composed from:
  - channel sync backlog
  - stale snapshot counts
  - open reconciliation drifts
  - latest reconciliation timing
  - latest TikTok ingress receipt
  - latest TikTok replay summary via admin activity audit
- shipped admin UI secondary route:
  - `/channels/health`
- ops overview links that route operators into channel health without adding a new top-level nav item
- Playwright browser verification harness for the admin SPA

## Backend Changes

- `OpsApplicationService` now exposes channel-health summaries for `SHOPEE` and `TIKTOK_SHOP`.
- Channel posture status is derived as:
  - `UNAVAILABLE` when real-mode connector configuration is invalid
  - `DEGRADED` when config is valid but backlog, stale snapshots, or open drifts are non-zero
  - `HEALTHY` otherwise
- Channel sync and reconciliation repositories now expose per-channel query support needed for posture aggregation.
- TikTok replay operations now emit admin activity audit records so the channel-health API can surface the latest replay action without inventing a separate replay detail endpoint.

## Frontend And Verification Changes

- `apps/admin-ui` now treats channel health as a shipped secondary workflow route rather than an unwired experiment.
- The page renders contract-backed posture cards with connector mode, config state, backlog, stale snapshots, open drifts, ingress receipt summary, and replay summary.
- A Playwright suite now covers:
  - login
  - refresh-cookie bootstrap
  - role redirects
  - outbox retry
  - reconciliation trigger and drift resolve
  - benchmark drill-down
  - channel-health route

## Verification

Passed:

- `npm test`
- `npm run build`
- `npm run test:e2e`
- `.\mvnw -pl apps/api -am -DskipTests compile`
- `.\testing\k6\Run-BenchmarkSuite.ps1 -ValidateFixtures`

Blocked by environment:

- `docker compose up -d`
  - failed because the Docker Desktop Linux engine pipe was unavailable on this machine
- `.\mvnw test`
  - module and non-container tests start, but all API integration tests stop at Testcontainers startup because Docker is unavailable
- full K6 suite with promoted evidence
  - not executed because the benchmark API/runtime stack could not be brought up without Docker-backed dependencies

## Release Readiness Outcome

- outcome: `inconclusive due environment`
- frontend route, browser verification, and Java compile wiring are green
- Docker-backed backend integration proof and full benchmark evidence refresh still require a machine with working Docker/Testcontainers support
