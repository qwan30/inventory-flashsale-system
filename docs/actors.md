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
- receive outbound sync attempts and contribute reconciliation facts

Current gap:

- `WEB` and `APP` still use local persisted sync and inbound snapshot behavior
- `SHOPEE` supports real-mode outbound sync and live inbound reconciliation reads
- no second marketplace connector exists yet

### Application Services

Current role:

- `ReservationApplicationService` orchestrates reservation, confirm, release, and expiry flows
- `OrderApplicationService` orchestrates order status updates

These services are the application boundary for cross-module coordination.

### Background Schedulers

Current role:

- `ReservationExpiryScheduler` releases expired active reservations
- `OutboxPublisherScheduler` publishes pending outbox events
- `ChannelSyncScheduler` dispatches pending channel sync attempts
- `ReconciliationScheduler` runs scheduled inventory reconciliation
- `OpsAlertDispatchScheduler` sends external alert notifications

These are the current automated operational actors inside the system.

### Operator / Admin

Current role:

- operationally monitors system health, inventory correctness, and event backlog
- manages infrastructure and benchmark execution outside the application
- authenticates into the admin API to manage campaigns and run remediation actions

Current gap:

- secure admin and operator APIs now exist in the repository
- there is still no implemented admin UI in this repository

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
- internal support or operations tools for remediation and manual review
- second-marketplace actors such as TikTok Shop once the connector slice lands

These are target roles only and are not yet implemented.
