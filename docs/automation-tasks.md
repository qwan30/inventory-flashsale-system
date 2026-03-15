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

### External Alert Delivery Sweep

Current task:

- evaluate current ops alerts on a schedule
- send transition notifications for newly active alerts
- resend reminders for still-active alerts after the configured interval
- send clear notifications when a previously notified alert becomes inactive
- persist delivery state and failures without blocking business flows

Current implementation:

- `OpsAlertDispatchScheduler`
- `OpsAlertDeliveryService`
- `alert_delivery_state`

### Benchmark Evidence Workflow

Current task:

- reset benchmark state before each scenario run
- execute the declarative K6 suite against the running API
- capture transient manifest, report, summary, comparison, and scenario summaries
- optionally promote one vetted run into curated evidence automatically
- maintain a durable evidence catalog for promoted runs

Current implementation:

- `Run-BenchmarkSuite.ps1`
- `Reset-BenchmarkState.ps1`
- `suite.json`
- `testing/k6/evidence/index.json`

Outcome:

- benchmark runs are reproducible, curated evidence can be promoted automatically, and promoted runs remain discoverable for scale-decision review

## Planned Operational Automations

These are planned ideas only, not implemented tasks.

### Drift Or Backlog Alerting

Target purpose:

- extend the current generic webhook path into richer vendor-specific alert routing, escalation, or observability stack integration

## Notes

- This document is about backend operational automation inside or around the application.
- It does not describe Codex recurring automations or chat-agent workflow automation.
