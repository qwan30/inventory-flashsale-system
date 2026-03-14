# Actors

**Last Updated:** 2026-03-15

## Current Actors

### Shopper

Current role:

- requests inventory visibility
- attempts to reserve stock during a flash sale
- confirms purchase through the reservation flow
- indirectly triggers release or expiry if checkout is abandoned

Current system touchpoints:

- reserve endpoint
- confirm endpoint
- inventory read endpoint

### Sales Channels

Current channels:

- `WEB`
- `APP`
- `SHOPEE`

Current role:

- identify the origin of a reservation or order lifecycle event
- route request validation through channel-specific adapters

Current gap:

- channel adapters are still mock validators
- they do not yet perform real external synchronization or reconciliation

### Application Services

Current role:

- `ReservationApplicationService` orchestrates reservation, confirm, release, and expiry flows
- `OrderApplicationService` orchestrates order status updates

These services are the application boundary for cross-module coordination.

### Background Schedulers

Current role:

- `ReservationExpiryScheduler` releases expired active reservations
- `OutboxPublisherScheduler` publishes pending outbox events

These are the current automated operational actors inside the system.

### Operator / Admin

Current role:

- operationally monitors system health, inventory correctness, and event backlog
- manages infrastructure and benchmark execution outside the application

Current gap:

- there is no implemented admin API or UI in this repository
- operator workflows are inferred from backend responsibilities, not delivered as product features

### Downstream Systems

Current role:

- consume Kafka events emitted from the outbox
- examples include shipment, notification, analytics, or marketplace sync services

Current gap:

- these consumers are not implemented in the current repository
- the repo only publishes event messages for them

## Target Additional Actors

Target future actors may include:

- channel integration workers for marketplace sync
- reconciliation jobs that compare central inventory with channel state
- internal support or operations tools for remediation and manual review

These are target roles only and are not yet implemented.
