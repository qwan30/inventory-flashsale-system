import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const campaignId = __ENV.CAMPAIGN_ID || "campaign-demo-001";
const sku = __ENV.SKU || "SKU-DEMO-001";

http.setResponseCallback(http.expectedStatuses(200, 201, 409, 423));

export const options = {
  scenarios: {
    contention: {
      executor: "constant-vus",
      vus: 50,
      duration: "30s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.3"],
    http_req_duration: ["p(95)<1000"],
  },
};

export default function () {
  const idempotencyKey = `k6-hot-${__VU}-${__ITER}`;
  const payload = JSON.stringify({
    sku,
    channel: "WEB",
    quantity: 1,
  });

  const response = http.post(
    `${baseUrl}/api/v1/flash-sales/${campaignId}/reservations`,
    payload,
    {
      headers: {
        "Content-Type": "application/json",
        "X-Idempotency-Key": idempotencyKey,
      },
    },
  );

  check(response, {
    "reservation accepted or rejected safely": (r) => [200, 201, 409, 423].includes(r.status),
  });
  sleep(0.1);
}
