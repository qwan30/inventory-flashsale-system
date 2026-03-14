import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const retryEventId = __ENV.RETRY_EVENT_ID;

export const options = {
  scenarios: {
    backlogPolling: {
      executor: "constant-vus",
      vus: 5,
      duration: "20s",
      exec: "pollBacklog",
    },
    optionalRetry: {
      executor: "constant-vus",
      vus: retryEventId ? 1 : 0,
      duration: "20s",
      exec: "retryFailedEvent",
    },
  },
};

export function pollBacklog() {
  const response = http.get(`${baseUrl}/api/v1/ops/outbox/backlog`);
  check(response, {
    "backlog endpoint healthy": (r) => r.status === 200,
  });
  sleep(0.5);
}

export function retryFailedEvent() {
  const response = http.post(`${baseUrl}/api/v1/ops/outbox/${retryEventId}/retry`, null);
  check(response, {
    "retry endpoint accepts valid event states": (r) => [200, 404, 409].includes(r.status),
  });
  sleep(1);
}
