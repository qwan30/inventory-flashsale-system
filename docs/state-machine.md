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
- repeated keyed release returns the stored release response

## Campaign State Machine

Current states:

- `DRAFT`
- `ACTIVE`
- `ENDED`

Current implemented transition semantics:

- admin APIs now expose the campaign lifecycle directly
- `DRAFT -> ACTIVE` through admin activation
- `DRAFT -> ENDED` through admin end
- `ACTIVE -> ENDED` through admin end
- reservation acceptance requires `ACTIVE`
- reservation acceptance also requires current time within campaign window

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
- repeated keyed transition returns the stored transition response

Rejected transitions:

- `PENDING -> SHIPPED`
- any backward move such as `PAID -> PENDING`
- any undefined future status transition
- a different idempotency key for an already applied keyed transition

## Outbox State Machine

Current states:

- `PENDING`
- `PUBLISHED`
- `FAILED`

Allowed transitions:

- new event -> `PENDING`
- successful publish -> `PUBLISHED`
- failed publish attempt -> `FAILED`

Implemented retry cycle:

- scheduler and ops retry both move eligible failed events back to `PENDING`

## Channel Sync State Machine

Current states:

- `PENDING`
- `SYNCED`
- `FAILED`

Allowed transitions:

- scheduled sync attempt -> `PENDING`
- successful adapter publish -> `SYNCED`
- transient or permanent adapter failure -> `FAILED`
- transient retry reset -> `PENDING`

Current notes:

- transient failures receive `next_attempt_at`
- permanent failures stay `FAILED` until a future code change adds manual sync retry

## Reconciliation Drift State Machine

Current states:

- `OPEN`
- `RESOLVED`

Allowed transitions:

- detected mismatch -> `OPEN`
- operator resolution -> `RESOLVED`

## Reconciliation Run State Machine

Current states:

- `RUNNING`
- `COMPLETED`
- `FAILED`

Allowed transitions:

- new manual or scheduled run -> `RUNNING`
- successful scan completion -> `COMPLETED`
- orchestration failure during scan -> `FAILED`

Current notes:

- `trigger_type` distinguishes `MANUAL` and `SCHEDULED`
- only the latest scheduled run failure should activate the scheduler-failure alert surface
