# API Contract

**Last Updated:** 2026-03-15

## Current Public Endpoints

### `POST /api/v1/flash-sales/{campaignId}/reservations`

Purpose:

- create or replay a reservation for a flash sale campaign

Headers:

- required `X-Idempotency-Key`
- optional `X-Correlation-Id`

Request body:

```json
{
  "sku": "SKU-DEMO-001",
  "channel": "WEB",
  "quantity": 1
}
```

Success response:

- current implementation returns `200 OK`

Response shape:

```json
{
  "reservationId": "string",
  "campaignId": "string",
  "sku": "string",
  "channel": "WEB",
  "quantity": 1,
  "status": "ACTIVE",
  "expiresAt": "2026-03-15T00:00:00Z",
  "inventory": {
    "sku": "string",
    "availableQty": 99,
    "reservedQty": 1,
    "soldQty": 0,
    "version": 1
  },
  "remainingCampaignQty": 49
}
```

Current failure classes:

- `400` validation or missing header
- `404` campaign or inventory not found
- `409` campaign, stock, quota, or reservation conflict
- `423` resource lock timeout

### `POST /api/v1/reservations/{reservationId}/confirm`

Purpose:

- confirm an active reservation into an order

Headers:

- required `X-Idempotency-Key`
- optional `X-Correlation-Id`

Success response:

- current implementation returns `200 OK`

Response shape:

```json
{
  "reservationId": "string",
  "orderId": "string",
  "orderStatus": "PENDING"
}
```

Current failure classes:

- `400` missing header
- `404` reservation or order not found
- `409` expired reservation, already confirmed with different key, or invalid reservation state
- `423` resource lock timeout

### `POST /api/v1/reservations/{reservationId}/release`

Purpose:

- release an active reservation back to inventory

Headers:

- optional `X-Idempotency-Key`
- optional `X-Correlation-Id`

Success response:

- current implementation returns `200 OK`

Response shape:

```json
{
  "reservationId": "string",
  "status": "RELEASED",
  "inventory": {
    "sku": "string",
    "availableQty": 100,
    "reservedQty": 0,
    "soldQty": 0,
    "version": 2
  }
}
```

Current failure classes:

- `404` reservation or inventory not found
- `409` confirmed reservation cannot be released, or a keyed release was already processed with a different key
- `423` resource lock timeout

### `GET /api/v1/inventory/{sku}`

Purpose:

- read central inventory for a SKU

Headers:

- optional `X-Correlation-Id`

Success response:

- current implementation returns `200 OK`

Response shape:

```json
{
  "sku": "string",
  "availableQty": 100,
  "reservedQty": 0,
  "soldQty": 0,
  "version": 0
}
```

Current failure classes:

- `404` inventory not found

### `POST /api/v1/orders/{orderId}/status`

Purpose:

- advance order status

Headers:

- optional `X-Idempotency-Key`
- optional `X-Correlation-Id`

Request body:

```json
{
  "status": "PAID"
}
```

Success response:

- current implementation returns `200 OK`

Response shape:

```json
{
  "orderId": "string",
  "reservationId": "string",
  "channel": "WEB",
  "status": "PAID"
}
```

Current failure classes:

- `400` validation error
- `404` order not found
- `409` invalid transition, or a keyed transition was already processed with a different key

### `POST /api/v1/admin/auth/login`

Purpose:

- authenticate an admin or operator user and issue JWT access plus refresh tokens

Headers:

- optional `X-Correlation-Id`

Request body:

```json
{
  "username": "admin",
  "password": "Admin123!"
}
```

Success response:

```json
{
  "accessToken": "string",
  "accessTokenExpiresAt": "2026-03-15T00:15:00Z",
  "refreshToken": "string",
  "refreshTokenExpiresAt": "2026-03-22T00:00:00Z",
  "username": "admin",
  "displayName": "System Admin",
  "role": "ADMIN"
}
```

Current failure classes:

- `401` invalid credentials

### `POST /api/v1/admin/auth/refresh`

Purpose:

- rotate a refresh token and issue a new access plus refresh token pair

Request body:

```json
{
  "refreshToken": "string"
}
```

Current failure classes:

- `401` invalid or expired refresh token

### `POST /api/v1/admin/auth/logout`

Purpose:

- revoke the current refresh token for an authenticated admin or operator session

Headers:

- required `Authorization: Bearer <token>`
- optional `X-Correlation-Id`

Request body:

```json
{
  "refreshToken": "string"
}
```

### `GET /api/v1/admin/campaigns`

Purpose:

- list flash sale campaigns for admin management

Headers:

- required `Authorization: Bearer <token>` with role `ADMIN`
- optional `X-Correlation-Id`

### `POST /api/v1/admin/campaigns`

Purpose:

- create a draft flash sale campaign for an existing SKU

Headers:

- required `Authorization: Bearer <token>` with role `ADMIN`
- optional `X-Correlation-Id`

Request body:

```json
{
  "id": "campaign-admin-001",
  "sku": "SKU-DEMO-001",
  "startsAt": "2026-03-16T00:00:00Z",
  "endsAt": "2026-03-16T02:00:00Z",
  "quota": 15
}
```

### `PUT /api/v1/admin/campaigns/{campaignId}`

Purpose:

- update a draft flash sale campaign before activation

### `POST /api/v1/admin/campaigns/{campaignId}/activate`

Purpose:

- transition a draft campaign to `ACTIVE`

### `POST /api/v1/admin/campaigns/{campaignId}/end`

Purpose:

- transition a draft or active campaign to `ENDED`

### `GET /api/v1/admin/campaigns/{campaignId}/audits`

Purpose:

- read immutable audit entries for admin campaign lifecycle actions

### `GET /api/v1/admin/ops/alerts`

Purpose:

- read the secured admin or operator alert surface

Headers:

- required `Authorization: Bearer <token>` with role `ADMIN` or `OPERATOR`
- optional `X-Correlation-Id`

### `GET /api/v1/ops/alerts`

Purpose:

- inspect current app-level operational alert conditions

Headers:

- required `Authorization: Bearer <token>` with role `ADMIN` or `OPERATOR`
- optional `X-Correlation-Id`

Success response:

```json
[
  {
    "code": "OUTBOX_FAILED_BACKLOG",
    "severity": "WARN",
    "status": "INACTIVE",
    "message": "Failed outbox backlog breached threshold",
    "currentValue": "0",
    "threshold": "10",
    "observedAt": "2026-03-15T00:00:00Z"
  }
]
```

### `GET /api/v1/ops/outbox/backlog`

Purpose:

- inspect current outbox backlog counts

Headers:

- required `Authorization: Bearer <token>` with role `ADMIN` or `OPERATOR`
- optional `X-Correlation-Id`

Success response:

```json
{
  "pendingCount": 1,
  "failedCount": 0,
  "retryableFailedCount": 0
}
```

### `POST /api/v1/ops/outbox/{eventId}/retry`

Purpose:

- reset a failed outbox event to `PENDING` for replay

Headers:

- required `Authorization: Bearer <token>` with role `ADMIN` or `OPERATOR`
- optional `X-Correlation-Id`

Success response:

```json
{
  "eventId": "string",
  "status": "PENDING",
  "attempts": 2,
  "nextAttemptAt": null,
  "lastError": null
}
```

Current failure classes:

- `404` outbox event not found
- `409` published outbox event cannot be retried

### `POST /api/v1/ops/reconciliation/runs`

Purpose:

- run reconciliation against persisted channel snapshots immediately

Headers:

- required `Authorization: Bearer <token>` with role `ADMIN` or `OPERATOR`
- optional `X-Correlation-Id`

Success response:

```json
{
  "runId": "string",
  "triggerType": "MANUAL",
  "status": "COMPLETED",
  "scannedSkuCount": 1,
  "scannedSnapshotCount": 3,
  "openDriftCount": 0,
  "failureMessage": null,
  "completedAt": "2026-03-15T00:00:00Z"
}
```

### `GET /api/v1/ops/reconciliation/drifts`

Purpose:

- list currently open reconciliation drifts

Headers:

- required `Authorization: Bearer <token>` with role `ADMIN` or `OPERATOR`
- optional `X-Correlation-Id`

Success response:

```json
[
  {
    "driftId": "string",
    "runId": "string",
    "channel": "WEB",
    "sku": "SKU-DEMO-001",
    "centralInventory": {
      "availableQty": 98,
      "reservedQty": 2,
      "soldQty": 0
    },
    "observedInventory": {
      "availableQty": 99,
      "reservedQty": 1,
      "soldQty": 0
    },
    "status": "OPEN",
    "resolutionNote": null,
    "resolvedAt": null
  }
]
```

### `POST /api/v1/ops/reconciliation/{driftId}/resolve`

Purpose:

- mark a reconciliation drift as resolved with an operator note

Headers:

- required `Authorization: Bearer <token>` with role `ADMIN` or `OPERATOR`
- optional `X-Correlation-Id`

Request body:

```json
{
  "resolutionNote": "Snapshot stale after resync"
}
```

Current failure classes:

- `400` validation error
- `404` drift not found

## Current Error Envelope

All handled errors use this response shape:

```json
{
  "timestamp": "2026-03-15T00:00:00Z",
  "status": 409,
  "error": "Conflict",
  "code": "FLASH_SALE_QUOTA_EXCEEDED",
  "message": "Flash sale quota is exhausted",
  "path": "/api/v1/flash-sales/campaign-demo-001/reservations",
  "correlationId": "string"
}
```

Notes:

- `X-Correlation-Id` is echoed back or generated if missing
- secured admin and ops endpoints also return `401` or `403` through the same error envelope
- unexpected exceptions return `500 INTERNAL_ERROR`

## Current Gaps In Public API Surface

Not yet implemented:

- omnichannel sync APIs
- benchmark evidence summary APIs
- admin or operator UI
