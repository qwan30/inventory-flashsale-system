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
- `409` confirmed reservation cannot be released
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
- `409` invalid transition

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
- unexpected exceptions return `500 INTERNAL_ERROR`

## Current Gaps In Public API Surface

Not yet implemented:

- admin APIs for campaign management
- omnichannel sync APIs
- reconciliation APIs
- outbox retry or operational remediation APIs
- operator reporting APIs for benchmark or drift analysis
