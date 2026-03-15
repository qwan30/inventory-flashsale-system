# 2026-03-15 Shopee First Sandbox Connector Slice

## Scope Delivered

This slice replaces Shopee’s mock-only transport with a sandbox-ready real connector while keeping the system a modular monolith and preserving existing shopper-facing APIs and schema.

Delivered in code:

- per-channel inbound snapshot resolution in `ChannelSyncService`
- separate local channel validation from sync transport so Shopee can stay locally validated while its sync path swaps by mode
- real-mode Shopee outbound sync via a `RestClient`-backed connector
- real-mode Shopee live inbound stock reads for reconciliation
- mock-mode fallback for local/dev/default flows
- deterministic Shopee error classification into transient vs permanent sync failures
- fail-fast Shopee real-mode configuration validation
- reconciliation protection against false sold-only drift when live Shopee stock does not expose sold quantity

## Implementation Details

### 1) Per-channel inbound and sync seams

- `ChannelInboundPort` now mirrors `ChannelSyncPort` by exposing `channel()` and per-channel snapshot fetch.
- `ChannelSyncService` resolves inbound ports and sync ports by `SalesChannel`.
- `WEB` and `APP` now use dedicated persisted inbound adapters.
- Shopee mock inbound remains persisted and is active only for `app.channel.shopee.mode=mock`.
- Shopee live inbound is active only for `app.channel.shopee.mode=real`.
- Mock validation and mock sync transport are no longer coupled in the same Shopee class.

### 2) Shopee real connector

- `ShopeeRestChannelClient` uses `RestClient` with shared signing/auth code for:
  - `/api/v2/product/get_item_list`
  - `/api/v2/product/get_item_base_info`
  - `/api/v2/product/get_model_list`
  - `/api/v2/product/update_stock`
- SKU resolution works by:
  - `item_sku` for non-variant items
  - `model_sku` for variant items
- The connector rejects ambiguous mappings, missing mappings, and unsupported multi-location seller stock payloads as permanent failures.
- Real outbound sync updates Shopee `seller_stock` from local `availableQty`.

### 3) Required Shopee config

The original plan listed `mode`, `baseUrl`, `partnerId`, `shopId`, `accessToken`, `connectTimeout`, and `readTimeout`.

Implementation added one more required field:

- `partnerKey`

Reason:

- Shopee’s official shop-API signing contract requires `partner_key` to compute `sign` with HMAC-SHA256.
- Without `partnerKey`, the connector would not be sandbox-ready against the real Shopee contract.

### 4) Reconciliation behavior

- `ShopeeLiveChannelInboundGateway` maps live Shopee stock into `ChannelInventorySnapshotView` with `soldQtyComparable=false`.
- `OpsApplicationService.isDrift(...)` always compares available/reserved, but compares sold quantity only when the inbound snapshot marks sold as comparable.
- `OpsApplicationService.observedSoldQty(...)` falls back to central sold quantity for non-comparable live Shopee payloads, preventing false sold-only drift records.
- Persisted `channel_inventory_snapshot` rows still back backlog and staleness metrics after successful outbound sync.

## Verification Evidence

Focused verification:

```powershell
.\mvnw --% -pl modules/channel,apps/api -am test -Dtest=ChannelSyncServiceTest,ShopeeConnectorConfigurationValidatorTest,ShopeeSigningSupportTest,ShopeeSandboxConnectorIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

Result:

- `BUILD SUCCESS`
- verified:
  - per-channel inbound port resolution
  - deterministic signing output
  - fail-fast real-mode config validation
  - Shopee real-mode outbound sync hits the HTTP connector
  - Shopee real-mode reconciliation uses live inbound reads
  - sold-only differences do not create false Shopee drifts

Full repository gate:

```powershell
.\mvnw --% test
```

Result:

- `BUILD SUCCESS`

## Notes For Future Sessions

- If Shopee later exposes authoritative sold quantity in a stable stock API, remove the sold comparability guard and compare sold directly for live Shopee reconciliation.
- If multi-location seller stock must be supported, extend the connector with an explicit location mapping rule instead of guessing.
- Keep `partnerKey` out of the repo and source it from runtime secrets only.
