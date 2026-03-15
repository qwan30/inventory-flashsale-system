# 2026-03-15 Alert Delivery And Benchmark Automation Slice

## Scope Delivered

This slice completes the roadmap's Phase A and the benchmark-runner portion of Phase B without changing shopper-facing APIs.

Delivered in code:

- generic webhook-based external alert delivery with default-safe disabled behavior
- persisted `alert_delivery_state` tracking observed status, notified status, last send time, last error, and consecutive failures
- scheduled alert dispatch that sends:
  - transition notifications for newly active alerts
  - reminder notifications for still-active alerts after the configured interval
  - clear notifications when a previously notified alert becomes inactive
- fail-fast alert delivery configuration validation when delivery is enabled
- benchmark runner support for:
  - `-PromoteIfPassed`
  - optional `-CommitSha`
  - generated `summary.md`
  - generated `comparison.json`
  - durable promoted-evidence cataloging in `testing/k6/evidence/index.json`
- canonical docs updates for the current roadmap, alert delivery surface, and benchmark evidence workflow

## Files And Boundaries

- no shopper-facing route changed
- app-owned schema change is limited to `alert_delivery_state`
- alert delivery orchestration lives in `apps/api`
- benchmark runner automation remains centered in `testing/k6/`
- no new `SalesChannel` values or connector contracts were introduced

## Verification

Focused alert-delivery gate:

```powershell
.\mvnw --% -pl apps/api -am test -Dtest=OpsAlertDeliveryServiceTest,AlertDeliveryConfigurationValidatorTest,WebhookAlertDeliveryPublisherTest,ShopeeConnectorConfigurationValidatorTest -Dsurefire.failIfNoSpecifiedTests=false
```

Result:

- `BUILD SUCCESS`

Benchmark runner verification:

```powershell
powershell -ExecutionPolicy Bypass -File .\testing\k6\Run-BenchmarkSuite.ps1 -ValidateFixtures
```

Result:

- fixture validation passed

Repository gate:

```powershell
.\mvnw --% test
```

Result:

- `BUILD SUCCESS`

## Notes For Future Sessions

- external alert delivery is disabled by default; enabling it requires `app.alerts.delivery.webhook-url` plus positive timeout and reminder settings
- promoted benchmark evidence is now discoverable through `testing/k6/evidence/index.json`
- use the related audit record in `docs/04_audit_remediation/` before proposing replication, partitioning, or topology changes
