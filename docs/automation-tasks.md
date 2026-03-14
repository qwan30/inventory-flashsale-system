# Automation Tasks

**Last Updated:** 2026-03-15

## Current Implemented Automated Tasks

### Reservation Expiry Sweep

Current task:

- periodically find `ACTIVE` reservations whose `expires_at` is in the past
- release inventory safely through the application service

Current implementation:

- `ReservationExpiryScheduler`

Outcome:

- reservation becomes `EXPIRED`
- stock returns to available inventory
- flash sale reserved quota is reduced

### Outbox Publish Sweep

Current task:

- periodically load pending outbox rows
- reset retryable failed rows back to `PENDING`
- publish them to Kafka
- mark rows `PUBLISHED` or `FAILED`

Current implementation:

- `OutboxPublisherScheduler`

Outcome:

- durable domain events are pushed to downstream consumers

### Channel Sync Sweep

Current task:

- periodically load pending channel sync attempts
- reset retryable transient failures back to `PENDING`
- publish outbound sync commands through the registered channel adapters
- persist the latest successful channel inventory snapshot

Current implementation:

- `ChannelSyncScheduler`

Outcome:

- the system retains a per-channel snapshot that reconciliation can compare against central inventory

### Inventory Reconciliation Sweep

Current task:

- periodically create a reconciliation run inside the monolith
- compare central inventory to persisted channel snapshots
- refresh or create open drift records without auto-resolving them

Current implementation:

- `ReconciliationScheduler`

Outcome:

- reconciliation evidence is captured continuously and unresolved drifts remain operator-visible

### In-App Alert Evaluation

Current task:

- evaluate current operational breaches from outbox backlog, channel sync backlog, open drifts, stale snapshots, and scheduled reconciliation failures
- expose those conditions through an ops API and metrics

Current implementation:

- `OpsAlertService`

## Planned Operational Automations

These are planned ideas only, not implemented tasks.

### Benchmark Report Generation

Target purpose:

- run K6 suites, collect performance results, and produce repeatable benchmark evidence

### Drift Or Backlog Alerting

Target purpose:

- push app-level operational breaches into an external alerting stack or notification channel

## Notes

- This document is about backend operational automation inside or around the application.
- It does not describe Codex recurring automations or chat-agent workflow automation.
