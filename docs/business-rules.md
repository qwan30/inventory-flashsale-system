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
- expiry reuses the stock-return semantics of release
- release and expiry both return reserved stock to available stock

## Order Rules

Current rules:

- orders are created in `PENDING`
- valid transitions are only `PENDING -> PAID -> SHIPPED`
- repeating the same status is allowed as a no-op
- invalid transitions are rejected as conflicts

## Eventing Rules

Current rules:

- reservation creation records `inventory.reservation.created`
- release and expiry record `inventory.reservation.released`
- reservation confirmation records `order.created`
- order status updates record order lifecycle events
- outbox rows are durable before publish is attempted

Current gap:

- publish failure handling exists, but automated retry semantics are still limited

## Target Future Rules

Target future rules not yet implemented:

- reconciliation rules for central inventory versus channel-side state
- channel sync failure classification and remediation policy
- operator workflow rules for manual correction or replay
