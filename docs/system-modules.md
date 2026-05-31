# System Modules

**Last Updated:** 2026-05-30

## Module Ownership

### `modules/common`

Current responsibilities:

- shared exception hierarchy
- API error envelope
- time abstraction
- common persistence timestamps
- HTTP header constants

Target extension direction:

- remain a shared support module, not a business rules module

### `modules/channel`

Current responsibilities:

- identify supported sales channels
- validate reservation requests through channel adapters
- support `WEB`, `APP`, `SHOPEE`, and `TIKTOK_SHOP`
- provide mock sync behavior for internal/local channels and mock marketplace modes
- provide conditional real-mode outbound sync for `SHOPEE` and `TIKTOK_SHOP`
- provide persisted inbound snapshots for mock marketplace modes
- provide live inbound inventory reads for Shopee and TikTok real modes
- persist outbound channel sync attempts and channel inventory snapshots
- store reconciliation runs and reconciliation drifts
- support scheduled reconciliation inputs and staleness evaluation

Current connector posture:

- `WEB` and `APP` use in-process mock sync ports and persisted snapshot reads.
- `SHOPEE` defaults to mock mode and switches to a signed real connector when `app.channel.shopee.mode=real` with base URL, partner ID, partner key, shop ID, access token, and timeouts configured.
- `TIKTOK_SHOP` defaults to mock mode and switches to a signed real connector when `app.channel.tik-tok.mode=real` with base URL, app key, app secret, shop cipher, access token, ingress secret, and timeouts configured.
- Reconciliation can compare against live marketplace reads in real mode or persisted snapshots in mock mode.
- Alerting and channel health are exposed through app/operator APIs; there is no separate observability service boundary.

Target extension direction:

- act as the bounded integration layer for omnichannel synchronization
- keep marketplace connectors behind channel ports rather than splitting services

### `modules/flashsale`

Current responsibilities:

- flash sale campaign entity and repository
- campaign status handling
- active-window validation
- quota reservation, release, and confirm rules

Target extension direction:

- stay the source of campaign rules and quota semantics

### `modules/inventory`

Current responsibilities:

- central inventory item state
- stock reservation state
- reservation lookup and expiry query support
- optimistic locking on inventory rows
- stock arithmetic for reserve, release, and confirm

Target extension direction:

- continue to own inventory correctness and central stock truth

### `modules/order`

Current responsibilities:

- order entity and repository
- allowed order status transitions
- reservation-to-order link

Target extension direction:

- remain the owner of order lifecycle invariants
- may later absorb richer order integration or partitioning work if scale demands it

### `modules/outbox`

Current responsibilities:

- durable outbox event persistence
- event payload serialization
- persisted `event_version`
- versioned outbox envelopes for Kafka publication
- scheduled batch publish to Kafka
- publish status tracking with `PENDING`, `PUBLISHED`, and `FAILED`
- delayed retry scheduling through `next_attempt_at`
- manual retry reset support for failed events

Current gap:

- downstream compatibility still depends on the current contract fixtures and simulator workflow staying in sync with source events

Target extension direction:

- provide stronger operational resilience and retry behavior without weakening correctness

### `apps/api`

Current responsibilities:

- Spring Boot entrypoint
- HTTP controllers
- application service orchestration
- marketplace connector implementations for Shopee and TikTok real modes
- signed TikTok ingress controllers for inventory and order-status callbacks
- admin TikTok ingress replay controller
- configuration properties
- Spring Security configuration with JWT auth for admin and ops APIs
- admin auth, refresh-token, campaign, ops, channel-health, benchmark evidence, and ops copilot API surfaces
- correlation ID filter
- Redis lock manager
- Flyway migrations
- schedulers

Target extension direction:

- remain the deployment boundary while the system stays a modular monolith
- keep admin/operator workflows as application-layer orchestration over module services

## Module Interaction Pattern

Current design intent:

- controllers stay thin
- business rules live in domain entities and application services
- cross-module integration happens through application services or bounded services
- inventory correctness has priority over convenience abstractions
- marketplace ingress delegates into central inventory/order semantics instead of creating a parallel source of truth

Current operator workflow ownership:

- `OpsApplicationService` composes reconciliation, outbox remediation, channel health, and channel drill-down responses.
- `AdminChannelController` exposes channel-health summary and detail endpoints for `SHOPEE` and `TIKTOK_SHOP`.
- `AdminTikTokIngressController` exposes audited TikTok ingress replay.
- benchmark evidence APIs read promoted evidence artifacts; they do not run load tests from request handlers.
- `OpsCopilotService` is advisory-only and does not execute retries, replays, or drift resolution actions automatically.
