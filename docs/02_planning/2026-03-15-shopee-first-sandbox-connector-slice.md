# 2026-03-15 Shopee-First Sandbox Connector Slice

## Classification

- Task size: `standard feature`
- Execution model: approved implementation with one serial prep bead, then two parallel tracks
- Architectural guardrails:
  - keep the system as a modular monolith
  - no shopper-facing REST API changes
  - no schema changes in this slice
  - preserve current `WEB` and `APP` behavior

## Objective

Replace the current mock-only `SHOPEE` transport with a sandbox-ready real connector while keeping existing local and test defaults stable through `mock` mode.

This slice delivers:

- per-channel inbound snapshot resolution
- Shopee sandbox HTTP outbound inventory sync
- Shopee live inbound stock reads for reconciliation in `real` mode
- default-safe mock wiring for local and existing test flows
- durable implementation and milestone documentation

## Verified External Contract

Verified against Shopee Open Platform documentation on 2026-03-15:

- Authorization and authentication guide:
  - sandbox authorization host `https://openplatform.sandbox.test-stable.shopee.sg`
  - shop API sign base string order: `partner_id + api path + timestamp + access_token + shop_id`
- Product stock docs:
  - outbound stock update endpoint path `/api/v2/product/update_stock`
  - inbound stock read endpoint path `/api/v2/product/get_item_base_info`
  - sandbox API host `https://partner.test-stable.shopeemobile.com`
  - stock updates can modify `seller_stock` only
  - live stock reads expose `stock_info_v2.summary_info.total_available_stock` and `total_reserved_stock`

## Assumptions

- `SHOPEE` is the only real connector in this slice.
- Sandbox readiness is the goal; no production rollout or secret material lands in the repo.
- Reservation validation remains local; this slice only changes outbound sync and reconciliation-time inbound reads.
- Internal SKU is matched to Shopee by seller-managed SKU semantics for the sandbox slice.
- Unsupported remote product shapes in this slice, including cases the connector cannot safely map, fail as permanent sync errors.
- Live Shopee reads are authoritative for available and reserved quantities. Sold quantity remains derived from local persisted state where Shopee does not expose an equivalent stock field.

## Serial Prep Bead

Single owner responsibilities before parallel execution:

- persist this plan
- refactor `ChannelInboundPort` from a single global gateway into a per-channel contract
- update `ChannelSyncService` to resolve inbound ports by `SalesChannel`
- add `ApplicationProperties.Channel.Shopee`:
  - `mode=mock|real`
  - `baseUrl`
  - `partnerId`
  - `shopId`
  - `accessToken`
  - `connectTimeout`
  - `readTimeout`
- add the shared Shopee client seam and DTO mapping surface that both inbound and outbound paths will reuse
- seed TDD coverage for real-mode sync, reconciliation, and fail-fast configuration behavior

## Parallel Tracks

### Track 1

Owner scope: `apps/api/**` Shopee HTTP connector implementation

Deliverables:

- `RestClient`-based Shopee sandbox client
- signing and auth query construction
- real Shopee outbound `ChannelSyncPort`
- conditional bean wiring on `app.channel.shopee.mode`
- deterministic exception mapping:
  - timeout, upstream 5xx, retryable upstream failures -> `TransientChannelSyncException`
  - auth, 4xx contract failures, unsupported payload -> `PermanentChannelSyncException`

### Track 2

Owner scope: `modules/channel/**`, reconciliation flow, tests, and durable implementation docs

Deliverables:

- per-channel inbound gateway resolution
- persisted inbound adapters for `WEB` and `APP`
- Shopee live inbound adapter in `real` mode
- reconciliation updates so Shopee compares central state against the live inbound view while snapshot persistence still powers backlog and staleness tracking
- implementation record in `docs/03_implementation/`
- concise milestone note in `docs/05_history/`

## Test Strategy

### Regression Safety

- existing integration tests continue to pass in default `mock` mode
- `WEB` and `APP` flows remain unchanged

### Shopee Connector Coverage

- signing is deterministic
- request mapping matches Shopee sandbox contract
- 2xx stock update success marks attempts `SYNCED`
- 4xx or auth failures are permanent
- 5xx and timeout failures are transient

### Spring Integration Coverage

- in `real` mode with a stubbed Shopee server, a reservation-driven sync reaches `SYNCED`
- snapshot persistence still updates after successful outbound sync
- reconciliation reads live Shopee stock and opens drift when remote values differ
- startup fails fast when `mode=real` but required Shopee config is missing

## Completion Criteria

- `.\mvnw test` passes
- plan persisted in `docs/02_planning/`
- shipped work recorded in `docs/03_implementation/`
- milestone recap recorded in `docs/05_history/`
- `docs/00_index.md` updated so future sessions can discover the new planning and implementation artifacts
