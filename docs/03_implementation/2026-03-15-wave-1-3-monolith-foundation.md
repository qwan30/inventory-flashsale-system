# 2026-03-15 Wave 1-3 Monolith Foundation

## Scope Delivered

This change set implements the monolith-first foundation across Wave 1 through Wave 3 of the master plan without changing the existing shopper-facing routes.

Delivered in code:

- persisted idempotency replay for reservation release and order status updates
- backward-compatible support for optional `X-Idempotency-Key` on release and order status APIs
- channel sync attempt persistence and retry handling derived from local outbox events
- persisted channel inventory snapshots for `WEB`, `APP`, and `SHOPEE`
- operator APIs for outbox backlog, failed-event retry, reconciliation runs, open drifts, and drift resolution
- reconciliation persistence for runs and drifts
- expanded integration coverage for keyed replay, channel sync retry, reconciliation, and ops remediation

## Files And Boundaries

- shopper-facing routes were preserved; only optional idempotency header handling was added to existing mutation endpoints
- new operator routes live under `/api/v1/ops`
- sync and reconciliation persistence live inside `modules/channel`
- generic mutation replay persistence lives in `apps/api` as application-support infrastructure
- schema additions are limited to:
  - `operation_idempotency`
  - `channel_sync_attempt`
  - `channel_inventory_snapshot`
  - `inventory_reconciliation_run`
  - `inventory_reconciliation_drift`

## Evidence

- `.\mvnw test` passed on 2026-03-15 after the changes landed

## Still Remaining From The Roadmap

- replace mock channel transports with real marketplace connectors
- add scheduled reconciliation plus alerting instead of operator-triggered runs only
- expand K6 artifact capture into a durable benchmark program
- keep scale-out decisions evidence-gated after benchmark results
