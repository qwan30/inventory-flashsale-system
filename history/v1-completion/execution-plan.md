# Execution Plan: V1 Completion

Epic: v1-completion-2026-03-15
Generated: 2026-03-15

## Tracks

| Track | Agent | Scope | Purpose |
| --- | --- | --- | --- |
| 1 | BlueLake | `modules/outbox/**`, `apps/api/src/main/java/com/codex/flashsale/events/**`, `testing/contracts/**` | Versioned event contracts, schema fixtures, runnable simulators |
| 2 | GreenCastle | `modules/channel/**`, `apps/api/src/main/java/com/codex/flashsale/connector/**`, `apps/api/src/main/java/com/codex/flashsale/channel/**`, `apps/api/src/main/resources/db/migration/**` | TikTok Shop connector, ingress, idempotent inbound receipts |
| 3 | RedStone | `apps/api/src/main/java/com/codex/flashsale/admin/**`, `apps/api/src/main/java/com/codex/flashsale/alerts/**`, `apps/api/src/main/java/com/codex/flashsale/controller/**`, `apps/api/src/main/java/com/codex/flashsale/api/**`, `apps/admin-ui/**`, `testing/k6/**` | Admin browser auth, benchmark evidence APIs, routing extensions, admin UI, benchmark suite/reporting |

## Cross-Track Dependencies

- Track 1 must define the final event envelope before Track 3 finishes benchmark evidence APIs.
- Track 2 must land TikTok channel/config names before Track 3 finishes admin UI channel reporting.
- Track 3 can scaffold the admin UI immediately, but final benchmark and TikTok views depend on Track 2 and Track 1 contracts.

## Integration Rules

- Keep shopper-facing reservation, confirm, release, inventory, and order APIs backward compatible.
- Keep Kafka topic and aggregate key stable.
- Do not change locking semantics or transaction boundaries without benchmark evidence.
- Use one serial integration gate after each track batch to resolve cross-scope DTO, config, and docs updates.
