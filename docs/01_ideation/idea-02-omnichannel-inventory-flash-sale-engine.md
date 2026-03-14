# Idea 02: Omnichannel Inventory & Flash Sale Engine

## Intent

Build an omnichannel inventory and flash sale system that preserves inventory correctness across multiple sales channels such as Shopee, Web, and App, while remaining stable under flash sale traffic spikes.

This file is an ideation artifact. It describes the target problem and candidate technical directions. It is not a statement that every item below is already implemented in the current repository.

## Business Pain Points

- Overselling:
  Inventory has only one unit left, but two customers complete purchase successfully at nearly the same time due to race conditions.
- Data inconsistency:
  Inventory numbers diverge across Web, App, and marketplace channels, causing operational confusion and customer trust issues.

## Target Functional Requirements

- Centralized inventory:
  Maintain a single real-time source of truth for stock across all channels.
- Flash sale mechanism:
  Support limited-quantity product sales during defined time windows.
- Soft reservation:
  Temporarily hold stock for up to 10 minutes when a customer enters cart or checkout, then release it automatically if the purchase is not completed.
- Order status synchronization:
  Synchronize order lifecycle changes such as `PENDING -> PAID -> SHIPPED` across connected systems.

## Candidate Technical Highlights

These are candidate approaches for planning and implementation. Some already exist in the current codebase, while others are future decisions.

- Concurrency control:
  Use Redis distributed locking to ensure only one stock-decrement path wins for a SKU at a given moment.
- Optimistic locking:
  Use database row versioning to detect conflicting inventory updates.
- Event-driven integration:
  Publish events such as `OrderCreated` to Kafka so downstream systems like shipment or notification can react asynchronously.
- Database scaling:
  Consider order-table partitioning or sharding if growth makes query performance or maintenance unacceptable.
- Topology options:
  Keep the modular monolith by default; only move toward microservices if scale or organizational boundaries justify the added complexity.

## Suggested Tech Stack

- Backend:
  Java 17 or 21, Spring Boot, Spring Cloud only if a service split becomes necessary.
- Data:
  MySQL for durable state, Redis for locking and fast reservation support.
- Messaging:
  Kafka for event propagation.
- Performance testing:
  K6 or JMeter for high-load validation.

## Metrics For CV Or Delivery Goals

- Handle `1000 orders/second` with average latency below `200ms`.
- Demonstrate `0% overselling` during high-load tests.

## Planning Notes

- The current repository already aligns with several parts of this idea:
  centralized inventory, flash sale windows, soft reservation, order status flow, Redis locking, optimistic locking, Kafka outbox, and K6 scripts.
- The next planning step should separate:
  current implemented capabilities,
  hardening work still needed,
  and optional scale-up investments such as sharding, replication, or service decomposition.
- Any future execution plan should keep inventory correctness and idempotency above topology or infrastructure complexity.
