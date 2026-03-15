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

2. Build the reactor once so module artifacts and compiler metadata are current:

```powershell
.\mvnw clean install -DskipTests
```

3. Run the API with benchmark profile:

```powershell
.\mvnw -f .\apps\api\pom.xml spring-boot:run -Dspring-boot.run.profiles=benchmark
```

4. Run benchmark suite:

```powershell
.\testing\k6\Run-BenchmarkSuite.ps1 -BaseUrl http://localhost:8080 -SpringProfile benchmark -PromoteIfPassed
```

Optional commit override:

```powershell
.\testing\k6\Run-BenchmarkSuite.ps1 -BaseUrl http://localhost:8080 -SpringProfile benchmark -PromoteIfPassed -CommitSha e2e3644
```

5. Inspect generated `manifest.json`, `report.json`, `summary.md`, and `comparison.json`.
6. Auto-promotion only occurs for a vetted run where:
   - `suiteStatus` is `PASSED`
   - every `scenarioResults[*].status` is `PASSED`
   - `businessChecks.passed` is `true`
   - `report.json`, `manifest.json`, `summary.md`, `comparison.json`, and all scenario summaries are copied unchanged
7. `testing/k6/evidence/index.json` is updated when auto-promotion succeeds so future sessions can enumerate curated evidence sets without scanning directories manually.

Optional fixture validation:

```powershell
.\testing\k6\Run-BenchmarkSuite.ps1 -ValidateFixtures
```

## Artifact Contracts

Transient artifacts are generated under:

- `testing/k6/artifacts/<timestamp>/manifest.json`
- `testing/k6/artifacts/<timestamp>/report.json`
- `testing/k6/artifacts/<timestamp>/summary.md`
- `testing/k6/artifacts/<timestamp>/comparison.json`
- `testing/k6/artifacts/<timestamp>/<scenario>.summary.json`

Curated evidence is copied to:

- `testing/k6/evidence/<timestamp>-<commit>/`
- `testing/k6/evidence/index.json`

The promoted directory must keep the same `manifest.json`, `report.json`, `summary.md`, `comparison.json`, and scenario summary structure.
The evidence index catalogs promoted runs by timestamp and commit, and points at the durable copied files under `testing/k6/evidence/`.

## Evidence Gate

Use promoted evidence as the decision gate for scale/topology work.

- No scale-out or topology recommendation should be accepted without at least one promoted evidence set from this workflow.
- The first promoted baseline is required to close the benchmark-evidence epic.
- The repo's current informational baseline target is `testing/k6/evidence/20260315-133859-e2e3644/report.json`.

## Integration Checkpoints

Runner behavior expected in this repository:

- `Run-BenchmarkSuite.ps1` resets benchmark state per scenario via `Reset-BenchmarkState.ps1 -Scenario <name>`.
- Runner output uses the timestamped directory structure documented above.
- `Run-BenchmarkSuite.ps1` accepts `-BaseUrl`, `-ArtifactRoot`, `-SuitePath`, `-SpringProfile`, `-ValidateFixtures`, `-PromoteIfPassed`, and `-CommitSha`.
- `Run-BenchmarkSuite.ps1` now compares later runs against `suite.json.baselineTarget` and keeps that comparison informational only.
- Promotion is now a built-in runner option behind `-PromoteIfPassed`.
