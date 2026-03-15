# 2026-03-15 Benchmark Evidence Program

## Summary

Benchmark documentation now defines a two-tier evidence model:

- transient outputs in `testing/k6/artifacts/<timestamp>/`
- curated promoted baselines in `testing/k6/evidence/<timestamp>-<commit>/`

## What Changed

- benchmark reset and base seeding are now implemented outside the app boundary through SQL and `Reset-BenchmarkState.ps1`
- benchmark profile settings are now formalized in `application-benchmark.yml`
- the runner now loads declarative suite metadata, emits manifest/report files, and performs business invariant checks through existing APIs
- benchmark operator flow now explicitly includes `benchmark` profile execution and evidence promotion
- project and system docs now state an evidence gate for scale/topology decisions
- benchmark evidence directory structure is now documented as durable project memory

## Verification

- fixture validation passed through `Run-BenchmarkSuite.ps1 -ValidateFixtures`
- benchmark docs now reflect current runner inputs (`-BaseUrl`, `-ArtifactRoot`, `-SuitePath`, `-SpringProfile`, `-ValidateFixtures`) and current manual promotion flow
- `.\mvnw test` still fails in this environment because Docker is unavailable for Testcontainers-backed API integration tests
- full benchmark suite execution was not completed here because `k6` is not installed locally

## How To Reuse This Next Session

- start with `testing/k6/README.md` for operator runbook and artifact contracts
- use `docs/03_implementation/2026-03-15-benchmark-evidence-program.md` for slice boundaries and integration assumptions
