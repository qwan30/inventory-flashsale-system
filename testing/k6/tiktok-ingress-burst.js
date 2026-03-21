import crypto from "k6/crypto";
import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const sku = __ENV.SKU || "SKU-DEMO-001";
const ingressSecret = __ENV.TIKTOK_INGRESS_SECRET || "tiktok-ingress-secret";

http.setResponseCallback(http.expectedStatuses(200, 201, 401));

export const options = {
  scenarios: {
    ingressBurst: {
      executor: "constant-vus",
      vus: 5,
      duration: "20s",
    },
  },
};

function sign(timestamp, payload) {
  return crypto.hmac("sha256", ingressSecret, `${timestamp}.${payload}`, "hex");
}

export default function () {
  const timestamp = `${Math.floor(Date.now() / 1000)}`;
  const payload = JSON.stringify({
    receiptId: `k6-tiktok-${__VU}-${__ITER}`,
    sku,
    availableQty: 90,
    reservedQty: 5,
    soldQty: 5,
  });

  const response = http.post(`${baseUrl}/api/v1/channel-ingress/tiktok/inventory`, payload, {
    headers: {
      "Content-Type": "application/json",
      "X-TikTok-Timestamp": timestamp,
      "X-TikTok-Signature": sign(timestamp, payload),
    },
  });

  check(response, {
    "tiktok ingress accepted": (result) => [200, 201].includes(result.status),
  });
  sleep(0.2);
}
