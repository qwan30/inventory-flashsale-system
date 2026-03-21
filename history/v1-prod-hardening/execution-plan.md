# Execution Plan: Production Hardening

Epic: v1-prod-hardening-2026-03-16
Generated: 2026-03-16

## Scope

- Finish target: production hardening on top of the current dirty worktree.
- Preserve the modular monolith, inventory correctness, idempotency, and existing shopper/admin contracts.
- Treat existing uncommitted TikTok/admin/benchmark work as baseline to preserve.

## Frozen Shared Foundations

Owned by orchestrator only during this epic:

- `apps/api/src/main/java/com/codex/flashsale/controller/**`
- shared backend DTO touchpoints under `apps/api/src/main/java/com/codex/flashsale/api/**`
- `apps/admin-ui/src/App.tsx`
- `apps/admin-ui/src/lib/api.ts`
- `apps/admin-ui/src/state/auth.tsx`
- `apps/admin-ui/src/components/ShellLayout.tsx`
- top-level durable docs and `docs/00_index.md`

## Channel Health Contract

Add one additive endpoint:

- `GET /api/v1/admin/channels/health`

Response shape:

- array of per-channel summaries for at least `SHOPEE` and `TIKTOK_SHOP`
- fields per summary:
  - `channel`
  - `status` as `HEALTHY`, `DEGRADED`, or `UNAVAILABLE`
  - `connectorMode`
  - `configValid`
  - `syncBacklogCount`
  - `staleSnapshotCount`
  - `openDriftCount`
  - `lastReconciliationAt`
  - `latestIngressReceipt`
  - `latestReplay`

Nested summary fields:

- `latestIngressReceipt`: `type`, `externalReceiptId`, `outcome`, `processedAt`
- `latestReplay`: `action`, `resourceId`, `outcome`, `createdAt`, `details`

Status rules:

- `UNAVAILABLE` when real-mode connector configuration is invalid
- `DEGRADED` when config is valid but any backlog, stale snapshot, or open drift count is non-zero
- `HEALTHY` otherwise

## Tracks

| Track | Agent | Scope | Purpose |
| --- | --- | --- | --- |
| 1 | BlueLake | `modules/channel/**`, `apps/api/src/main/java/com/codex/flashsale/application/**`, `apps/api/src/main/java/com/codex/flashsale/channel/**`, `apps/api/src/test/java/com/codex/flashsale/OpsAndChannelIntegrationTest.java` | Backend channel-health read model, aggregation, persistence queries, and tests |
| 2 | GreenCastle | `apps/admin-ui/src/views/channels/**`, `apps/admin-ui/src/views/ops/**`, `apps/admin-ui/src/views/channels/ChannelHealthPage.test.tsx` | Ship the contract-backed channel-health UI and operator links without touching shared routing/api/auth files |
| 3 | RedStone | `apps/admin-ui/playwright.config.ts`, `apps/admin-ui/e2e/**`, `apps/admin-ui/package.json`, `apps/admin-ui/package-lock.json` | Add Playwright browser verification for auth, remediation, benchmark detail, and channel health |

## Cross-Track Dependencies

- Track 1 defines the final channel-health response contract before orchestrator wires shared DTO/controller/API-client changes.
- Track 2 should implement the page against the agreed contract shape and leave shared route/api integration to orchestrator.
- Track 3 can prepare the Playwright harness immediately, but final channel-health coverage depends on orchestrator integrating Track 1 plus Track 2.

## Integration Rules

- Do not overwrite or revert existing dirty-worktree changes.
- Workers are not alone in the codebase and must not revert other edits.
- No new top-level navigation item for channel health.
- Keep campaign routes `ADMIN` only; ops, benchmarks, and channel health remain `ADMIN` and `OPERATOR`.
- If Docker-backed verification stays unavailable, record the blocked surface explicitly rather than claiming release readiness.
