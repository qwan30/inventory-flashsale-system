# Inventory Flash Sale System

Codex-first base project for an omnichannel inventory and flash sale engine. The repository uses a modular monolith on Java 21 + Spring Boot + Maven, with local infrastructure for MySQL, Redis, and Kafka.

## Stack

- Java 21
- Spring Boot 3
- Maven multi-module build
- MySQL 8 + Flyway
- Redis for distributed locking and reservation TTL support
- Kafka for outbox-driven domain events
- Testcontainers for integration tests
- K6 smoke tests

## Repository layout

```text
apps/api         Spring Boot deployable application
modules/common   Shared error handling, time, and request utilities
modules/channel  Internal sales channel abstraction and mock adapters
modules/flashsale Flash sale campaign rules and quota management
modules/inventory Inventory item and reservation logic
modules/order    Order lifecycle skeleton
modules/outbox   Outbox persistence and Kafka publisher
testing/k6       Smoke load scripts
```

## Run locally

1. Start infrastructure:

```powershell
docker compose up -d
```

2. Run the application:

```powershell
.\mvnw spring-boot:run -pl apps/api
```

3. Run tests:

```powershell
.\mvnw test
```

4. Run K6 smoke checks:

```powershell
k6 run .\testing\k6\hot-sku-contention.js
k6 run .\testing\k6\flash-sale-window.js
k6 run .\testing\k6\reservation-expiry.js
```

5. Run benchmark evidence suite:

```powershell
.\mvnw clean install -DskipTests
.\mvnw -f .\apps\api\pom.xml spring-boot:run -Dspring-boot.run.profiles=benchmark
.\testing\k6\Run-BenchmarkSuite.ps1 -BaseUrl http://localhost:8080 -SpringProfile benchmark -PromoteIfPassed
```

Use `-CommitSha <value>` when the current git commit is unavailable or when promoting evidence for a detached artifact review. Each suite run now emits `summary.md` and `comparison.json` alongside `manifest.json`, `report.json`, and per-scenario summaries. When `-PromoteIfPassed` is supplied, only runs where `suiteStatus` is `PASSED`, every `scenarioResults[*].status` is `PASSED`, and `businessChecks.passed` is `true` are copied automatically into `testing/k6/evidence/<timestamp>-<commit>/`.

Promoted evidence sets are cataloged in `testing/k6/evidence/index.json`, which records the curated evidence directory, its copied report/manifest paths, and the baseline target used for comparison.

The repo's current informational baseline target is `testing/k6/evidence/20260315-133859-e2e3644/report.json`.

## Key API endpoints

- `POST /api/v1/flash-sales/{campaignId}/reservations`
- `POST /api/v1/reservations/{reservationId}/confirm`
- `POST /api/v1/reservations/{reservationId}/release`
- `GET /api/v1/inventory/{sku}`
- `POST /api/v1/orders/{orderId}/status`

## Notes

- `everything-claude-code/` and `skills/` are ignored and excluded from the build.
- The project is configured for Codex usage through repo-local guidance in `AGENTS.md` and `.codex/`.
- Demo seed data is available with campaign `campaign-demo-001` and SKU `SKU-DEMO-001`.
- Evidence gate: do not propose topology or scale-out changes without promoted benchmark evidence from `testing/k6/evidence/`.
