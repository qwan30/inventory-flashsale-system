# 2026-05-30 Docs Truth Refresh

## Scope Delivered

This session refreshed stale canonical docs without changing production code.

Updated:

- `docs/system-modules.md`
- `docs/data-model.md`
- `docs/core-business-flows.md`
- `docs/00_index.md`

## Source Checks

Checked current source and graph context for:

- `SalesChannel`
- `ChannelSyncService`
- `ChannelReconciliationService`
- `TikTokIngressService`
- `OpsApplicationService`
- Shopee and TikTok connector packages
- TikTok ingress controllers and signature verifier
- Flyway migrations `V1` through `V10` for the affected schema claims

`npx.cmd gitnexus status` reported the index up to date at current commit `401a502` before documentation edits.

## Current Truth Captured

- Supported channels are `WEB`, `APP`, `SHOPEE`, and `TIKTOK_SHOP`.
- Shopee and TikTok both support mock mode and conditional real-mode outbound sync.
- Shopee and TikTok real modes include live inbound inventory reads for reconciliation.
- TikTok signed ingress supports inventory and order-status callbacks, receipt idempotency, and admin replay.
- Channel sync attempts, snapshots, reconciliation runs, reconciliation drifts, operation idempotency, admin security/audit tables, outbox event versioning, and TikTok ingress receipts are represented in the schema docs.
- Channel health is a shipped operator API for `SHOPEE` and `TIKTOK_SHOP`, backed by sync backlog, stale snapshots, drifts, connector config, latest reconciliation, TikTok ingress receipt, and replay audit data.

## Verification

- Re-read the updated docs for consistency.
- Reviewed the scoped diff for docs-only changes.
- Did not run Maven, npm, Playwright, Docker, or K6 because this was a documentation-only truth refresh.
