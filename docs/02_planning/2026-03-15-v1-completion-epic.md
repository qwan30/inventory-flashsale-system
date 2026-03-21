# 2026-03-15 V1 Completion Epic

## Objective

Complete the next high-value baseline gaps without leaving the modular monolith:

- versioned event contracts plus runnable downstream simulators
- second real marketplace connector via TikTok Shop
- authenticated external channel ingress with idempotent receipts
- admin/operator React SPA
- benchmark evidence summary APIs
- provider-aware alert routing
- expanded promoted benchmark evidence

## Guardrails

- central inventory remains the source of truth
- reservation, campaign, order, and outbox invariants remain intact
- current shopper APIs stay backward compatible
- replication, sharding, and service decomposition remain out of scope

## Execution Shape

- serial prep first: execution artifact, docs truth-up, file-scope lock
- parallel track 1: contracts and simulators
- parallel track 2: TikTok connector and ingress
- parallel track 3: admin backend, routing, React admin UI, benchmark reporting
- serial integration gates after each track batch

## Verification Targets

- `.\mvnw test`
- focused Spring integration coverage for TikTok ingress, browser-safe auth, benchmark evidence APIs, and routing
- frontend build plus smoke coverage for login, campaign ops, drift/backlog, and benchmark views
- promoted K6 evidence for the expanded scenario set
