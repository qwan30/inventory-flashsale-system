# System Modules

**Last Updated:** 2026-03-15

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
- provide mock sync adapters for `WEB`, `APP`, and `SHOPEE`
- persist outbound channel sync attempts and channel inventory snapshots
- store reconciliation runs and reconciliation drifts
- support scheduled reconciliation inputs and staleness evaluation

Current gap:

- no real marketplace transport or credentialed connector
- inbound facts are derived from persisted snapshots, not pulled from external systems
- alerting is exposed through the app only, not an external observability stack

Target extension direction:

- act as the bounded integration layer for omnichannel synchronization

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
- scheduled batch publish to Kafka
- publish status tracking with `PENDING`, `PUBLISHED`, and `FAILED`
- manual retry reset support for failed events

Current gap:

- failure handling is stronger, but it still lacks alerting and richer operator workflows beyond backlog inspection and retry

Target extension direction:

- provide stronger operational resilience and retry behavior without weakening correctness

### `apps/api`

Current responsibilities:

- Spring Boot entrypoint
- HTTP controllers
- application service orchestration
- configuration properties
- correlation ID filter
- Redis lock manager
- Flyway migrations
- schedulers

Target extension direction:

- remain the deployment boundary while the system stays a modular monolith

## Module Interaction Pattern

Current design intent:

- controllers stay thin
- business rules live in domain entities and application services
- cross-module integration happens through application services or bounded services
- inventory correctness has priority over convenience abstractions

Target gap:

- external omnichannel transport and alerting still need to mature on top of the implemented `channel` boundary
