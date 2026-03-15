# 2026-03-15 Shopee First Sandbox Connector Slice

## What Changed

Shopee now has a real sandbox connector path for outbound sync and live inbound reconciliation reads. `WEB` and `APP` remain on persisted/local inbound snapshots, and Shopee reconciliation ignores sold-only differences when live Shopee stock does not expose sold quantity.

## Evidence

- Integration verification command:
  `.\mvnw --% test`
- Result: full Maven test suite passed on 2026-03-15.

## Why This Matters

- Avoids false reconciliation drift caused by Shopee live APIs lacking sold quantity.
- Preserves channel-specific inbound behavior and keeps real-mode Shopee checks aligned to available/reserved stock correctness.
- Future real-mode setups must provide `partnerKey` because Shopee request signing requires it.

## Canonical Record

- See implementation detail: `docs/03_implementation/2026-03-15-shopee-first-sandbox-connector-slice.md`.
