# 2026-03-15 Discovery And Doc Bootstrap

## Summary

A search-first discovery pass was completed for the inventory and flash sale system, then the result was persisted into `docs/` as durable project context.

## What Was Established

- The repository is a Java 21 + Spring Boot 3 modular monolith with `apps/api` as the deployable app.
- Core domain modules are `common`, `channel`, `flashsale`, `inventory`, `order`, and `outbox`.
- The critical lifecycle is:
  `reserve -> confirm -> release/expire -> order status update -> outbox publish`
- Inventory correctness depends on both Redis SKU locking and optimistic locking on `inventory_item.version`.
- Reservation creation and confirmation use idempotency keys with durable database constraints.
- The main project map now lives in `docs/system-map.md`.
- The fast-start debug and implementation entrypoints now live in `docs/retrieval-guide.md`.

## Verification

- `.\mvnw test` passed during the discovery session.
- Integration tests cover reserve/confirm success, duplicate reservation idempotency, campaign window rejection, confirm idempotency, oversell protection, expiry restoration, and outbox publication.

## Follow-Up Signals

- Flyway warns that MySQL 8.4 is newer than its tested support window.
- Multiple `junit-platform.properties` files are present on the test classpath.
- Mockito emits a future JDK dynamic-agent warning during tests.

## How To Reuse This Next Session

- Start with `docs/00_index.md`.
- For architecture or change placement, read `docs/system-map.md`.
- For debugging or feature entrypoints, read `docs/retrieval-guide.md`.
- Add another history entry only when the next task produces durable conclusions or completes a meaningful milestone.
