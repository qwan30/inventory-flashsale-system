# 2026-03-15 Benchmark Evidence Program

## Scope Delivered

This slice turns the earlier K6 runner foundation into a durable benchmark evidence workflow with deterministic reset, repeatable benchmark profile settings, declarative suite metadata, and promotable evidence artifacts.

Delivered in code:

- deterministic benchmark reset through `testing/k6/Reset-BenchmarkState.ps1 -Scenario <name>`
- FK-safe reset SQL plus deterministic benchmark seed SQL under `testing/k6/sql/`
- `benchmark` Spring profile in `application-benchmark.yml` for repeatable scheduler and batch settings
- declarative suite metadata in `testing/k6/suite.json`
- timestamped transient artifacts under `testing/k6/artifacts/<timestamp>/`
- generated `manifest.json`, `report.json`, and per-scenario summary exports
- post-run business invariant checks using existing inventory and ops APIs only
- fixture validation support through `Run-BenchmarkSuite.ps1 -ValidateFixtures`
- curated evidence placeholders and operator promotion workflow under `testing/k6/evidence/`

Documented contracts:

- transient benchmark outputs belong under `testing/k6/artifacts/<timestamp>/`
- curated evidence is copied to `testing/k6/evidence/<timestamp>-<commit>/`
- operator workflow is:
  start Docker Compose,
  run API with `benchmark` profile,
  run benchmark suite,
  inspect manifest/report,
  promote one vetted run
- scale/topology recommendations are evidence-gated and require promoted baseline evidence

## Files And Boundaries

- no shopper-facing routes are introduced
- no schema migrations are introduced by this slice
- benchmark execution assets are centered in `testing/k6/`
- benchmark contracts are centered in `testing/k6/README.md`
- project-level benchmark invocation and evidence gate guidance is reflected in `README.md`
- cross-cutting benchmark guidance is updated in:
  `docs/configuration-rules.md`,
  `docs/non-functional-requirements.md`,
  `docs/automation-tasks.md`

## Integration Points

Current benchmark integration points:

- `Reset-BenchmarkState.ps1` applies base reset and seed SQL, then an optional scenario overlay from `testing/k6/sql/scenarios/<scenario>.sql`
- `Run-BenchmarkSuite.ps1` emits `manifest.json`, `report.json`, and scenario summaries under `testing/k6/artifacts/<timestamp>/`
- per-scenario reset is executed through `Reset-BenchmarkState.ps1 -Scenario <name>`
- `suite.json` declares seed, captured benchmark config keys, scenario order, post-run checks, and optional baseline target
- fixture validation is available through `Run-BenchmarkSuite.ps1 -ValidateFixtures`
- manifest config capture falls back to values from `application-benchmark.yml` when environment variables are not set

Current promotion behavior:

- promotion is an explicit operator copy step from transient artifacts into `testing/k6/evidence/<timestamp>-<commit>/`
- no built-in runner promotion flag exists in this slice

## Verification Notes

- fixture-only runner validation passed through `Run-BenchmarkSuite.ps1 -ValidateFixtures`
- manifest config capture was smoke-tested against `application-benchmark.yml`
- full `.\mvnw test` remains blocked in this environment because the API integration tests require Docker/Testcontainers
- full end-to-end benchmark execution remains blocked here because `k6` is not installed locally
