import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const username = __ENV.ADMIN_USERNAME || "admin";
const password = __ENV.ADMIN_PASSWORD || "Admin123!";

http.setResponseCallback(http.expectedStatuses(200, 201, 401));

export const options = {
  scenarios: {
    benchmarkReads: {
      executor: "constant-vus",
      vus: 2,
      duration: "15s",
    },
  },
};

export default function () {
  const loginResponse = http.post(
    `${baseUrl}/api/v1/admin/auth/login`,
    JSON.stringify({ username, password }),
    {
      headers: {
        "Content-Type": "application/json",
      },
    },
  );

  check(loginResponse, {
    "admin login returns token": (response) => response.status === 200,
  });

  if (loginResponse.status !== 200) {
    sleep(1);
    return;
  }

  const accessToken = loginResponse.json("accessToken");
  const headers = { Authorization: `Bearer ${accessToken}` };

  const listResponse = http.get(`${baseUrl}/api/v1/admin/ops/benchmarks/evidence`, {
    headers,
  });

  check(listResponse, {
    "benchmark evidence list is readable": (response) => response.status === 200,
  });

  const latestResponse = http.get(`${baseUrl}/api/v1/admin/ops/benchmarks/evidence/latest`, {
    headers,
  });

  check(latestResponse, {
    "latest benchmark evidence is readable": (response) => response.status === 200,
  });

  sleep(0.5);
}
