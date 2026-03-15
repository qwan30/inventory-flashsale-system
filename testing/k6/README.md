# K6 Benchmark Program

This folder stores repeatable benchmark scenarios and evidence workflows for the modular monolith.

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

## Operator Flow

1. Start infrastructure:

```powershell
docker compose up -d
```

2. Run the API with benchmark profile:

```powershell
.\mvnw spring-boot:run -pl apps/api -Dspring-boot.run.profiles=benchmark
```

3. Run benchmark suite:

```powershell
.\testing\k6\Run-BenchmarkSuite.ps1 -BaseUrl http://localhost:8080 -SpringProfile benchmark
```

4. Inspect generated `manifest.json` and `report.json`.
5. Promote one vetted run to curated evidence (copy, do not move):

```powershell
Copy-Item -Recurse .\testing\k6\artifacts\<timestamp> .\testing\k6\evidence\<timestamp>-<commit>
```

Optional fixture validation:

```powershell
.\testing\k6\Run-BenchmarkSuite.ps1 -ValidateFixtures
```

## Artifact Contracts

Transient artifacts are generated under:

- `testing/k6/artifacts/<timestamp>/manifest.json`
- `testing/k6/artifacts/<timestamp>/report.json`
- `testing/k6/artifacts/<timestamp>/<scenario>.summary.json`

Curated evidence is copied to:

- `testing/k6/evidence/<timestamp>-<commit>/`

The promoted directory must keep the same `manifest.json`, `report.json`, and scenario summary structure.

## Evidence Gate

Use promoted evidence as the decision gate for scale/topology work.

- No scale-out or topology recommendation should be accepted without at least one promoted evidence set from this workflow.
- The first promoted baseline is required to close the benchmark-evidence epic.

## Integration Checkpoints

Runner behavior expected in this repository:

- `Run-BenchmarkSuite.ps1` resets benchmark state per scenario via `Reset-BenchmarkState.ps1 -Scenario <name>`.
- Runner output uses the timestamped directory structure documented above.
- `Run-BenchmarkSuite.ps1` accepts `-BaseUrl`, `-ArtifactRoot`, `-SuitePath`, `-SpringProfile`, and `-ValidateFixtures`.
- Promotion is currently an operator copy step, not a built-in runner flag.
