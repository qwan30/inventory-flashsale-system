# 2026-03-21 CV-Safe Benchmark Claims

- what changed:
  qualified which benchmark and verification numbers are safe to reuse in a CV without overstating the repo's current proof level
- evidence:
  `docs/04_audit_remediation/2026-03-21-cv-evidence-and-star-bullets.md`
- durable conclusion:
  the strongest safe claims today are `5/5` promoted K6 scenarios passed, `100%` business checks, `0%` HTTP failure, hot-SKU throughput around `186 req/s`, and verified admin/ops browser coverage
- what future sessions should do:
  reuse the audited bullets for resume work and do not upgrade the claims to `1000 orders/s` or global `p95 < 200ms` until a fresh Docker-backed rerun proves them
