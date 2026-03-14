# State Machine

**Last Updated:** 2026-03-15

## Reservation State Machine

Current states:

- `ACTIVE`
- `CONFIRMED`
- `RELEASED`
- `EXPIRED`

Allowed transitions:

- create reservation -> `ACTIVE`
- confirm active reservation -> `CONFIRMED`
- release active reservation -> `RELEASED`
- expire active reservation -> `EXPIRED`

Rejected or blocked transitions:

- `CONFIRMED -> RELEASED`
- `CONFIRMED -> EXPIRED`
- confirm with a different idempotency key after already confirmed
- confirm after expiry time

Idempotent behaviors:

- repeated release on `RELEASED` or `EXPIRED` returns current state
- repeated confirm with the same confirm idempotency key returns the same order

## Campaign State Machine

Current states:

- `DRAFT`
- `ACTIVE`
- `ENDED`

Current implemented transition semantics:

- reservation acceptance requires `ACTIVE`
- reservation acceptance also requires current time within campaign window

Current gap:

- explicit transition workflows between `DRAFT`, `ACTIVE`, and `ENDED` are not yet exposed through public APIs

## Order State Machine

Current states:

- `PENDING`
- `PAID`
- `SHIPPED`

Allowed transitions:

- `PENDING -> PAID`
- `PAID -> SHIPPED`

No-op:

- same-state update returns without changing status

Rejected transitions:

- `PENDING -> SHIPPED`
- any backward move such as `PAID -> PENDING`
- any undefined future status transition

## Outbox State Machine

Current states:

- `PENDING`
- `PUBLISHED`
- `FAILED`

Allowed transitions:

- new event -> `PENDING`
- successful publish -> `PUBLISHED`
- failed publish attempt -> `FAILED`

Current gap:

- there is a `resetForRetry()` method, but the current application flow does not yet implement a documented retry cycle that moves failed events back to `PENDING`
