import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const campaignId = __ENV.CAMPAIGN_ID || "campaign-demo-001";
const sku = __ENV.SKU || "SKU-DEMO-001";
const channels = ["WEB", "APP", "SHOPEE", "TIKTOK_SHOP"];

http.setResponseCallback(http.expectedStatuses(200, 201, 400, 401, 403, 409, 423));

export const options = {
  scenarios: {
    mixedFlow: {
      executor: "constant-vus",
      vus: 10,
      duration: "20s",
    },
  },
};

export default function () {
  const channel = channels[(__VU + __ITER) % channels.length];
  const reserveKey = `k6-mixed-reserve-${channel}-${__VU}-${__ITER}`;
  const confirmKey = `k6-mixed-confirm-${channel}-${__VU}-${__ITER}`;

  const reserveResponse = http.post(
    `${baseUrl}/api/v1/flash-sales/${campaignId}/reservations`,
    JSON.stringify({ sku, channel, quantity: 1 }),
    {
      headers: {
        "Content-Type": "application/json",
        "X-Idempotency-Key": reserveKey,
      },
    },
  );

  check(reserveResponse, {
    "mixed channel reserve handled safely": (response) =>
      [200, 201, 409, 423].includes(response.status),
  });

  if (reserveResponse.status === 200 || reserveResponse.status === 201) {
    const reservation = reserveResponse.json();
    const confirmResponse = http.post(
      `${baseUrl}/api/v1/reservations/${reservation.reservationId}/confirm`,
      null,
      {
        headers: {
          "X-Idempotency-Key": confirmKey,
        },
      },
    );

    check(confirmResponse, {
      "mixed channel confirm handled safely": (response) =>
        [200, 201, 409, 423].includes(response.status),
    });
  }

  sleep(0.2);
}
