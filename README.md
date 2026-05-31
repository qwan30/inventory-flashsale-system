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
apps/api            Spring Boot deployable application
apps/admin-ui       React admin SPA for campaign, ops, benchmark, and channel health workflows
modules/common      Shared error handling, time, and request utilities
modules/channel     Internal sales channel abstraction, mock adapters, and Shopee/TikTok connectors
modules/flashsale   Flash sale campaign rules and quota management
modules/inventory   Inventory item and reservation logic
modules/order       Order lifecycle skeleton
modules/outbox      Outbox persistence and Kafka publisher
testing/k6          Smoke load scripts and benchmark evidence suite
testing/contracts   Versioned outbox event contracts and simulator harnesses
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

## Deployment & CI

The repository ships containers for the API and the admin UI so a simple-cloud container platform can deploy the stack.

### Environment

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`: standard MySQL connectivity.
- `REDIS_HOST`, `REDIS_PORT`: Redis lock support.
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap.
- `APP_SECURITY_JWT_SECRET`: must be at least 32 bytes for the API signing key.
- `VITE_API_BASE_URL`: admin UI runtime API endpoint (defaults to `http://localhost:8080`).
- `API_BACKEND_HOST`: nginx proxy target inside the admin UI container (defaults to `localhost:8080`).

### Build and Run the Containers

```
docker build -f apps/api/Dockerfile -t inventory-flashsale-api .
docker build -f apps/admin-ui/Dockerfile -t inventory-flashsale-admin-ui .
docker run -d --name flashsale-api -p 8080:8080 \
  -e DB_HOST=... -e REDIS_HOST=... -e KAFKA_BOOTSTRAP_SERVERS=... \
  inventory-flashsale-api
docker run -d --name flashsale-admin -p 3000:80 \
  -e VITE_API_BASE_URL=http://host.docker.internal:8080 \
  inventory-flashsale-admin-ui
```

Refer to the CI workflow at `.github/workflows/ci.yml` for the commands run on push/pull requests.
