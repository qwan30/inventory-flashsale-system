# Configuration Rules

**Last Updated:** 2026-03-15

## Runtime Dependencies

Current required infrastructure:

- MySQL
- Redis
- Kafka

Local default path:

- `docker compose up -d`

## Current Application Properties

### Data Stores

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.data.redis.host`
- `spring.data.redis.port`
- `spring.kafka.bootstrap-servers`

### Persistence And Migration

- Flyway is enabled
- Hibernate schema mode is `validate`
- application timezone is normalized to UTC for persistence and JSON serialization

### Reservation Rules

- `app.reservation.ttl`
- default `10m`

### Lock Rules

- `app.lock.wait-timeout`
- default `2s`
- `app.lock.lease-timeout`
- default `5s`

Guidance:

- lock tuning must preserve correctness first
- do not reduce lock safety just to improve raw throughput numbers

### Outbox Rules

- `app.outbox.publish-batch-size`
- default `50`
- `app.outbox.retry-delay`
- default `10s`
- `app.outbox.max-attempts`
- default `5`
- `app.kafka.topic`
- default `inventory-flashsale.events`

### Channel Sync Rules

- `app.channel.sync-batch-size`
- default `50`
- `app.channel.retry-delay`
- default `15s`
- `app.channel.max-attempts`
- default `3`

### Alert Rules

- `app.alerts.outbox-failed-threshold`
- default `10`
- `app.alerts.channel-sync-failed-threshold`
- default `10`
- `app.alerts.reconciliation-open-drift-threshold`
- default `5`
- `app.alerts.channel-snapshot-staleness`
- default `5m`
- `app.alerts.delivery.enabled`
- default `false`
- `app.alerts.delivery.webhook-url`
- required when delivery is enabled
- `app.alerts.delivery.connect-timeout`
- default `2s`
- `app.alerts.delivery.read-timeout`
- default `5s`
- `app.alerts.delivery.reminder-interval`
- default `15m`

### Scheduler Rules

- `app.scheduler.expired-reservation-delay`
- default `30s`
- `app.scheduler.outbox-delay`
- default `5s`
- `app.scheduler.channel-sync-delay`
- default `10s`
- `app.scheduler.reconciliation-delay`
- default `60s`
- `app.scheduler.alert-delivery-delay`
- default `30s`

Guidance:

- scheduler tuning should be benchmarked together with outbox backlog and expiry latency
- alert delivery must remain fail-safe and must never block reservation, outbox, channel sync, or reconciliation behavior

## Header Rules

- `X-Idempotency-Key` is required for reserve and confirm
- `X-Correlation-Id` is optional and echoed or generated automatically

## Environment Rules

### Local Development

- use docker compose services when local MySQL, Redis, or Kafka are needed
- run the app with `.\mvnw spring-boot:run -pl apps/api`

### Test

- integration tests use Testcontainers for MySQL, Redis, and Kafka
- backend validation should prefer `.\mvnw test`

### Benchmark Or Load Test

- K6 should target the running API instance
- benchmark environments must record the configuration values used for lock timing, batch size, scheduler cadence, and infrastructure endpoints
- benchmark runs should use the `benchmark` Spring profile for repeatable operator validation
- benchmark runs should persist manifest and report outputs under `testing/k6/artifacts/<timestamp>/`
- benchmark runs now also emit `summary.md` and `comparison.json` under `testing/k6/artifacts/<timestamp>/`
- promoted baselines should be copied to `testing/k6/evidence/<timestamp>-<commit>/`, ideally via `Run-BenchmarkSuite.ps1 -PromoteIfPassed`
- `testing/k6/evidence/index.json` is the durable catalog of promoted evidence directories and copied report paths

## Current Configuration Gaps

Not yet formalized:

- explicit operator-run config matrix for high-load validation
- vendor-specific alert transport integrations beyond the generic webhook delivery path
