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

### Scheduler Rules

- `app.scheduler.expired-reservation-delay`
- default `30s`
- `app.scheduler.outbox-delay`
- default `5s`
- `app.scheduler.channel-sync-delay`
- default `10s`
- `app.scheduler.reconciliation-delay`
- default `60s`

Guidance:

- scheduler tuning should be benchmarked together with outbox backlog and expiry latency

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

## Current Configuration Gaps

Not yet formalized:

- benchmark-specific config profiles
- explicit operator-run config matrix for high-load validation
