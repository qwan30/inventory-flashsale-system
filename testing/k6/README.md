# K6 Scenarios

This folder stores repeatable benchmark scenarios for the modular monolith.

## Scenarios

- `hot-sku-contention.js`
  Exercises reservation contention on one SKU.
- `flash-sale-window.js`
  Exercises active and inactive campaign windows.
- `reservation-expiry.js`
  Exercises expiry and manual release behavior.
- `outbox-backlog-recovery.js`
  Exercises ops backlog reads and optional failed-event retry.
- `reconciliation-load.js`
  Exercises reconciliation runs and drift listing under repeated load.

## Artifact Format

Export each run into a durable JSON artifact so future sessions can compare results.

Recommended command pattern:

```powershell
.\testing\k6\Run-BenchmarkSuite.ps1 -BaseUrl http://localhost:8080
```

Each artifact should preserve at least:

- request rate
- average and percentile latency
- failed request rate
- conflict or retry rate inferred from status codes
- oversell evidence from business assertions outside K6 when relevant
- backlog growth or reconciliation run counts for ops scenarios
- manifest metadata including git commit, scenario list, base URL, and key runtime config inputs
