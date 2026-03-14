import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
  scenarios: {
    reconciliationRuns: {
      executor: "constant-vus",
      vus: 3,
      duration: "30s",
      exec: "runReconciliation",
    },
    driftReads: {
      executor: "constant-vus",
      vus: 5,
      duration: "30s",
      exec: "readDrifts",
      startTime: "5s",
    },
  },
};

export function runReconciliation() {
  const response = http.post(`${baseUrl}/api/v1/ops/reconciliation/runs`, null);
  check(response, {
    "reconciliation run accepted": (r) => r.status === 200,
  });
  sleep(1);
}

export function readDrifts() {
  const response = http.get(`${baseUrl}/api/v1/ops/reconciliation/drifts`);
  check(response, {
    "drift listing healthy": (r) => r.status === 200,
  });
  sleep(0.5);
}
