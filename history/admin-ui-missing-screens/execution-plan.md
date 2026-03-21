# Execution Plan: Admin UI Missing Screens

Epic: admin-ui-missing-screens-2026-03-16
Generated: 2026-03-16

## Tracks

| Track | Agent | Scope | Purpose |
| --- | --- | --- | --- |
| 1 | BlueLake | `apps/admin-ui/src/views/CampaignsPage.tsx`, `apps/admin-ui/src/views/CampaignDetailPage.tsx`, `apps/admin-ui/src/views/CampaignAuditPage.tsx`, `apps/admin-ui/src/views/campaigns/**` | Campaign detail and audit workflow completion |
| 2 | GreenCastle | `apps/admin-ui/src/views/OpsPage.tsx`, `apps/admin-ui/src/views/OpsRemediationPage.tsx`, `apps/admin-ui/src/views/BenchmarksPage.tsx`, `apps/admin-ui/src/views/BenchmarkRunDetailPage.tsx`, `apps/admin-ui/src/views/ops/**`, `apps/admin-ui/src/views/benchmarks/**` | Ops remediation and benchmark detail workflow completion |

## Shared Foundation

Owned by orchestrator before parallel work:

- `apps/admin-ui/src/App.tsx`
- `apps/admin-ui/src/components/ShellLayout.tsx`
- `apps/admin-ui/src/lib/api.ts`
- `apps/admin-ui/src/state/auth.tsx`
- `apps/admin-ui/src/styles.css`
- `apps/admin-ui/src/App.test.tsx`
- `apps/admin-ui/src/test/**`

## Cross-Track Dependencies

- Shared route/auth/API/test groundwork lands first before either track starts editing page files.
- BlueLake and GreenCastle must not edit shared foundation files directly; any follow-up shared change is integrated by the orchestrator after review.

## Integration Rules

- Keep the top-level nav unchanged: `Campaigns`, `Ops`, `Benchmarks`.
- Treat campaign routes as `ADMIN` only in the SPA to match backend authorization.
- Treat ops and benchmark routes as available to both `ADMIN` and `OPERATOR`.
- Prefer the dedicated backend-first admin contracts that already exist; do not expand backend scope unless a real integration mismatch is discovered.
- Benchmark comparison must gracefully handle the current no-baseline fixture state without fabricating deltas.
