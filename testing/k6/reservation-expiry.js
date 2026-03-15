import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const campaignId = __ENV.CAMPAIGN_ID || "campaign-demo-001";
const sku = __ENV.SKU || "SKU-DEMO-001";

http.setResponseCallback(http.expectedStatuses(200, 201, 409));

export const options = {
  vus: 5,
  iterations: 20,
};

export default function () {
  const reserveResponse = http.post(
    `${baseUrl}/api/v1/flash-sales/${campaignId}/reservations`,
    JSON.stringify({ sku, channel: "APP", quantity: 1 }),
    {
      headers: {
        "Content-Type": "application/json",
        "X-Idempotency-Key": `expiry-${__VU}-${__ITER}`,
      },
    },
  );

  check(reserveResponse, {
    "reservation created": (r) => [200, 201].includes(r.status),
  });

  if (![200, 201].includes(reserveResponse.status)) {
    return;
  }

  const reservation = reserveResponse.json();
  sleep(Number(__ENV.EXPIRY_WAIT_SECONDS || 12));

  const releaseResponse = http.post(
    `${baseUrl}/api/v1/reservations/${reservation.reservationId}/release`,
    null,
  );

  check(releaseResponse, {
    "manual release accepted": (r) => [200, 409].includes(r.status),
  });
}
