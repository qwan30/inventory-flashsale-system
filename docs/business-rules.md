# Business Rules

**Last Updated:** 2026-03-15

## Inventory Correctness Rules

Current rules:

- stock cannot be reserved if `availableQty < requested quantity`
- stock cannot be released if `reservedQty < release quantity`
- stock cannot be confirmed if `reservedQty < confirm quantity`
- inventory changes are protected by both Redis SKU locking and optimistic locking

Target rule direction:

- these correctness rules must remain invariant even if channel integration, load, or data volume increases

## Flash Sale Rules

Current rules:

- a campaign must exist to reserve through the flash sale endpoint
- campaign SKU must match the requested SKU
- campaign status must be `ACTIVE`
- current time must be within campaign start and end time
- remaining campaign quota must be sufficient before reservation is accepted
- only `DRAFT` campaigns can be updated
- admin activation transitions a draft campaign to `ACTIVE`
- admin end transitions a draft or active campaign to `ENDED`

## Reservation Rules

Current rules:

- reservation quantity must be at least `1`
- reservation SKU must be non-blank
- reservation channel must be supported
- reserve requires `X-Idempotency-Key`
- repeated reserve with the same idempotency key returns the existing reservation
- reservation starts in `ACTIVE`
- reservation expiry time is `now + app.reservation.ttl`

## Confirm Rules

Current rules:

- confirm requires `X-Idempotency-Key`
- confirm uses reservation SKU locking before modifying stock
- same-key duplicate confirm returns the existing order
- different-key confirm on an already confirmed reservation is a conflict
- expired reservations cannot be confirmed
- only `ACTIVE` reservations can be confirmed

## Release And Expiry Rules

Current rules:

- confirmed reservations cannot be released
- repeated release or expiry is idempotent
- release accepts optional `X-Idempotency-Key`; same-key duplicates replay the stored response
- release with a different idempotency key after a keyed release is a conflict
- expiry reuses the stock-return semantics of release
- release and expiry both return reserved stock to available stock

## Order Rules

Current rules:

- orders are created in `PENDING`
- valid transitions are only `PENDING -> PAID -> SHIPPED`
- repeating the same status is allowed as a no-op
- order status update accepts optional `X-Idempotency-Key`; same-key duplicates replay the stored transition response
- a different idempotency key for an already applied keyed transition is a conflict
- invalid transitions are rejected as conflicts

## Admin Access Rules

Current rules:

- admin auth login requires valid seeded or persisted credentials
- admin refresh tokens are rotated on refresh and revoked on logout
- `/api/v1/admin/campaigns/**` requires role `ADMIN`
- `/api/v1/admin/ops/**` and `/api/v1/ops/**` require role `ADMIN` or `OPERATOR`
- admin campaign and ops mutations are written to immutable activity audit records

## Eventing Rules

Current rules:

- reservation creation records `inventory.reservation.created`
- release and expiry record `inventory.reservation.released`
- reservation confirmation records `order.created`
- order status updates record order lifecycle events
- outbox rows are durable before publish is attempted
- inventory-affecting outbox events also schedule channel sync attempts for all supported channels
- failed outbox rows may be reset to `PENDING` by scheduler retry or manual ops retry

Current gap:

- Shopee has a real sandbox-ready connector path, but a second marketplace connector is still missing

## Target Future Rules

Target future rules not yet implemented:

- connector-specific retry and credential policies for real marketplaces
- richer alert routing policy beyond the current generic webhook delivery path
