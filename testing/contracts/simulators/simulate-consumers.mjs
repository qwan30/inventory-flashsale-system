import fs from "node:fs";

if (process.argv.length < 3) {
  console.error("Usage: node simulate-consumers.mjs <envelope-json-path>");
  process.exit(1);
}

const envelope = JSON.parse(fs.readFileSync(process.argv[2], "utf8"));

const shipmentProjection = {
  stream: "shipment",
  aggregateId: envelope.aggregateId,
  shouldCreateShipment: envelope.eventType === "order.shipped",
  eventType: envelope.eventType,
  eventVersion: envelope.eventVersion,
  payload: envelope.payload,
};

const notificationProjection = {
  stream: "notification",
  aggregateId: envelope.aggregateId,
  template:
    envelope.eventType === "inventory.reservation.created"
      ? "reservation-created"
      : envelope.eventType === "order.created"
        ? "order-created"
        : envelope.eventType === "order.paid"
          ? "order-paid"
          : envelope.eventType === "order.shipped"
            ? "order-shipped"
            : "unknown",
  eventType: envelope.eventType,
  eventVersion: envelope.eventVersion,
  payload: envelope.payload,
};

const analyticsProjection = {
  stream: "analytics",
  aggregateType: envelope.aggregateType,
  aggregateId: envelope.aggregateId,
  eventType: envelope.eventType,
  eventVersion: envelope.eventVersion,
  occurredAt: envelope.occurredAt,
  dimensions: {
    channel: envelope.payload.channel ?? null,
    sku: envelope.payload.sku ?? null,
    status: envelope.payload.status ?? null,
  },
};

console.log(
  JSON.stringify(
    {
      shipment: shipmentProjection,
      notification: notificationProjection,
      analytics: analyticsProjection,
    },
    null,
    2,
  ),
);
