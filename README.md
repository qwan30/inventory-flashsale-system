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
