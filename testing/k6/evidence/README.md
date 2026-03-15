# Curated Benchmark Evidence

This directory stores promoted benchmark evidence only.

Use one folder per vetted run:

- `testing/k6/evidence/<timestamp>-<commit>/`

Each promoted folder should include:

- `manifest.json`
- `report.json`
- `<scenario>.summary.json` files copied from the transient artifact set

Do not commit transient `testing/k6/artifacts/` runs directly.
