# 2026-03-15 Evidence-Gated Scale Audit

## Evidence Reviewed

- promoted benchmark report:
  - `testing/k6/evidence/20260315-133859-e2e3644/report.json`
- promoted benchmark milestone note:
  - `docs/05_history/2026-03-15-first-promoted-benchmark-baseline.md`
- runtime hotspots:
  - `apps/api/src/main/java/com/codex/flashsale/application/ReservationApplicationService.java`
  - `apps/api/src/main/java/com/codex/flashsale/config/RedisLockManager.java`
  - `modules/outbox/src/main/java/com/codex/flashsale/outbox/OutboxService.java`
  - `modules/channel/src/main/java/com/codex/flashsale/channel/sync/ChannelSyncService.java`

## Benchmark Readout

Promoted scenario evidence currently shows:

- `hot-sku-contention`
  - average request duration about `164ms`
  - p95 about `920ms`
  - failed-request rate `0`
- `flash-sale-window`
  - average request duration about `70ms`
  - p95 about `220ms`
  - failed-request rate `0`
- `reservation-expiry`
  - average request duration about `48ms`
  - p95 about `176ms`
  - failed-request rate `0`
- `outbox-backlog-recovery`
  - average request duration about `6ms`
  - p95 about `8ms`
  - failed-request rate `0`
- `reconciliation-load`
  - average request duration about `8ms`
  - p95 about `26ms`
  - failed-request rate `0`

Business invariant evidence in the promoted report:

- `inventory_non_negative` passed
- `inventory_stock_conservation` passed
- outbox, alerts, and reconciliation drift ops endpoints remained available
- reported outbox backlog ended at `pending=0`, `failed=0`, `retryableFailed=0`

## Target Assessment

### `0% oversell`

- Current verdict: `go for the covered benchmark scenarios only`
- Reason:
  - the promoted evidence shows stock conservation and non-negative inventory after the benchmark suite
  - the repository also has concurrency integration coverage for oversell prevention
- Limitation:
  - this is not blanket proof for every future load shape or topology change

### `<200ms` average latency

- Current verdict: `partially met in the promoted scenario set`
- Reason:
  - all current scenario averages are below `200ms`
  - the hottest contention scenario is still close to the edge at about `164ms`
- Limitation:
  - `hot-sku-contention` p95 is about `920ms`, so tail behavior still needs attention before treating the target as generally comfortable

### `1000 orders/sec`

- Current verdict: `no-go due to insufficient evidence`
- Reason:
  - the promoted report does not establish sustained throughput at `1000 orders/sec`
  - the current evidence set proves correctness and latency behavior for the present benchmark shapes, not target-rate saturation

## Hotspot Findings

### Locking

- `RedisLockManager` still serializes by SKU with polling retries every `50ms`
- Recommendation:
  - keep current safety-first lock behavior
  - do not reduce wait or sleep safety parameters without new promoted evidence focused on lock contention tradeoffs

### Reservation Write Path

- `ReservationApplicationService` keeps inventory mutation, campaign quota updates, reservation persistence, and sync scheduling in one application-service flow
- Recommendation:
  - keep the current modular-monolith transaction shape
  - do not split this flow across services or async boundaries without new correctness evidence

### Outbox And Channel Sync Batching

- `OutboxService` and `ChannelSyncService` both rely on fixed page-sized batch loops with retry scheduling
- Recommendation:
  - batch-size tuning is a `go` only as benchmark-profile experimentation
  - do not change production defaults without benchmark evidence showing queue or scheduler pressure

## Go / No-Go Matrix

- lock tuning:
  - `no-go` for safety-reducing changes
  - `go` only for benchmark-profile experiments backed by promoted evidence
- DB/index work:
  - `go` for targeted investigation if later evidence shows backlog scans, retry lag, or reconciliation query pressure
- batching changes:
  - `go` for benchmark-profile experiments
  - `no-go` for unguarded production-default increases
- replication:
  - `no-go`
- partitioning:
  - `no-go`
- service decomposition:
  - `no-go`
- stay on the modular monolith:
  - `go`

## Conclusion

The current evidence supports staying on the modular monolith with correctness-first locking and evidence-gated tuning. The next justified work is stronger benchmark coverage and targeted benchmark-profile experimentation, not topology expansion.
