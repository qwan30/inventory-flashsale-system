import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const activeCampaignId = __ENV.ACTIVE_CAMPAIGN_ID || "campaign-demo-001";
const inactiveCampaignId = __ENV.INACTIVE_CAMPAIGN_ID || "campaign-ended-001";
const sku = __ENV.SKU || "SKU-DEMO-001";

http.setResponseCallback(http.expectedStatuses(200, 201, 404, 409, 423));

export const options = {
  scenarios: {
    activeWindow: {
      executor: "constant-vus",
      vus: 10,
      duration: "15s",
      exec: "activeWindow",
    },
    inactiveWindow: {
      executor: "constant-vus",
      vus: 10,
      duration: "15s",
      exec: "inactiveWindow",
      startTime: "5s",
    },
  },
};

function reserve(campaignId, channel) {
  return http.post(
    `${baseUrl}/api/v1/flash-sales/${campaignId}/reservations`,
    JSON.stringify({ sku, channel, quantity: 1 }),
    {
      headers: {
        "Content-Type": "application/json",
        "X-Idempotency-Key": `window-${campaignId}-${channel}-${__VU}-${__ITER}`,
      },
    },
  );
}

export function activeWindow() {
  const response = reserve(activeCampaignId, "APP");
  check(response, {
    "active campaign handled safely": (r) => [200, 201, 409, 423].includes(r.status),
  });
  sleep(0.2);
}

export function inactiveWindow() {
  const response = reserve(inactiveCampaignId, "WEB");
  check(response, {
    "inactive campaign rejected": (r) => [404, 409].includes(r.status),
  });
  sleep(0.2);
}
