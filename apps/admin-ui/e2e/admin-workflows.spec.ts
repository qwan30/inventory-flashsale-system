import { expect, test, type Page, type Route } from "@playwright/test";

type AdminSession = {
  accessToken: string;
  accessTokenExpiresAt: string;
  username: string;
  displayName: string;
  role: "ADMIN" | "OPERATOR";
};

type ReconciliationDrift = {
  driftId: string;
  runId: string;
  channel: string;
  sku: string;
  centralInventory: {
    availableQty: number;
    reservedQty: number;
    soldQty: number;
  };
  observedInventory: {
    availableQty: number;
    reservedQty: number;
    soldQty: number;
  };
  status: string;
  resolutionNote: string | null;
  resolvedAt: string | null;
};

type ReconciliationRun = {
  runId: string;
  triggerType: string;
  status: string;
  scannedSkuCount: number;
  scannedSnapshotCount: number;
  openDriftCount: number;
  failureMessage: string | null;
  createdAt: string;
  completedAt: string | null;
};

type MockOptions = {
  refreshSession?: AdminSession | null;
  loginSession?: AdminSession;
};

const ADMIN_SESSION: AdminSession = {
  accessToken: "admin-access-token",
  accessTokenExpiresAt: "2026-03-16T15:30:00Z",
  username: "admin",
  displayName: "System Admin",
  role: "ADMIN",
};

const OPERATOR_SESSION: AdminSession = {
  accessToken: "operator-access-token",
  accessTokenExpiresAt: "2026-03-16T15:30:00Z",
  username: "operator",
  displayName: "Operations User",
  role: "OPERATOR",
};

function json(route: Route, body: unknown, status = 200) {
  return route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

function empty(route: Route, status = 204) {
  return route.fulfill({ status });
}

function parsePath(url: string): string {
  return new URL(url).pathname;
}

function buildBenchmarkDetail(runId: string) {
  return {
    entry: {
      runId,
      timestamp: "2026-03-16T07:45:00Z",
      gitCommit: "abc1234",
      evidenceDir: "testing/k6/evidence/20260316-074500-abc1234",
      suiteStatus: "PASSED",
      businessChecksPassed: true,
      baselineTarget: "testing/k6/evidence/20260315-133859-e2e3644/report.json",
    },
    manifest: {
      runId,
      suiteStatus: "PASSED",
    },
    report: {
      summary: {
        averageLatencyMs: 152.1,
      },
    },
    comparison: null,
    summaryMarkdown: "# Benchmark Summary\n\n- Suite passed\n- No baseline comparison available",
    suiteSummary: {
      suiteStatus: "PASSED",
      businessChecksPassed: true,
      baselineTarget: "testing/k6/evidence/20260315-133859-e2e3644/report.json",
      baselineAvailable: false,
      baselineNote: "No baseline comparison is available for this run.",
    },
    scenarioSummaries: [
      {
        name: "mixed-channel-flow",
        status: "PASSED",
        averageLatencyMs: 145.3,
        p95LatencyMs: 201.8,
        failedRate: 0,
        checksRate: 1,
        postRunChecks: ["inventory_invariants", "ops_snapshots"],
      },
    ],
    scenarioComparisons: [
      {
        scenarioName: "mixed-channel-flow",
        available: false,
        note: "No baseline available for this scenario.",
        delta: null,
      },
    ],
  };
}

async function installApiMock(page: Page, options: MockOptions = {}) {
  const refreshSession = options.refreshSession ?? null;
  const loginSession = options.loginSession ?? ADMIN_SESSION;
  const benchmarkRunId = "run-123";
  const counters = {
    refreshCalls: 0,
    loginCalls: 0,
    retryCalls: 0,
    runReconciliationCalls: 0,
    resolveDriftCalls: 0,
  };

  const state: {
    outboxEvents: Array<{
      eventId: string;
      aggregateType: string;
      aggregateId: string;
      eventType: string;
      eventVersion: number;
      status: string;
      attempts: number;
      lastError: string | null;
      nextAttemptAt: string | null;
      createdAt: string;
      updatedAt: string;
    }>;
    runs: ReconciliationRun[];
    drifts: ReconciliationDrift[];
  } = {
    outboxEvents: [
      {
        eventId: "evt-failed-001",
        aggregateType: "reservation",
        aggregateId: "res-001",
        eventType: "inventory.reservation.created",
        eventVersion: 1,
        status: "FAILED",
        attempts: 2,
        lastError: "broker unavailable",
        nextAttemptAt: null,
        createdAt: "2026-03-16T07:01:00Z",
        updatedAt: "2026-03-16T07:03:00Z",
      },
    ],
    runs: [
      {
        runId: "run-existing-001",
        triggerType: "SCHEDULED",
        status: "COMPLETED",
        scannedSkuCount: 3,
        scannedSnapshotCount: 12,
        openDriftCount: 1,
        failureMessage: null,
        createdAt: "2026-03-16T07:00:00Z",
        completedAt: "2026-03-16T07:01:30Z",
      },
    ],
    drifts: [
      {
        driftId: "drift-001",
        runId: "run-existing-001",
        channel: "TIKTOK_SHOP",
        sku: "SKU-DEMO-001",
        centralInventory: { availableQty: 10, reservedQty: 1, soldQty: 2 },
        observedInventory: { availableQty: 9, reservedQty: 1, soldQty: 2 },
        status: "OPEN",
        resolutionNote: null,
        resolvedAt: null,
      },
    ],
  };

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const method = request.method();
    const path = parsePath(request.url());

    if (path === "/api/v1/admin/auth/refresh" && method === "POST") {
      counters.refreshCalls += 1;
      if (!refreshSession) {
        return route.fulfill({ status: 401, body: "refresh unavailable" });
      }
      return json(route, refreshSession);
    }

    if (path === "/api/v1/admin/auth/login" && method === "POST") {
      counters.loginCalls += 1;
      return json(route, loginSession);
    }

    if (path === "/api/v1/admin/auth/logout" && method === "POST") {
      return empty(route);
    }

    if (path === "/api/v1/admin/campaigns" && method === "GET") {
      return json(route, []);
    }

    if (path === "/api/v1/admin/ops/alerts" && method === "GET") {
      return json(route, []);
    }

    if (path === "/api/v1/admin/ops/outbox/backlog" && method === "GET") {
      return json(route, {
        pendingCount: 0,
        failedCount: state.outboxEvents.filter((event) => event.status === "FAILED").length,
        retryableFailedCount: state.outboxEvents.filter((event) => event.status === "FAILED").length,
      });
    }

    if (path === "/api/v1/admin/ops/outbox/events" && method === "GET") {
      return json(route, state.outboxEvents);
    }

    if (path.startsWith("/api/v1/admin/ops/outbox/") && path.endsWith("/retry") && method === "POST") {
      counters.retryCalls += 1;
      const eventId = path.split("/")[6];
      const event = state.outboxEvents.find((item) => item.eventId === eventId);
      if (!event) {
        return route.fulfill({ status: 404, body: "event not found" });
      }
      event.status = "PENDING";
      event.attempts += 1;
      event.lastError = null;
      event.updatedAt = "2026-03-16T07:12:00Z";
      return json(route, {
        eventId: event.eventId,
        status: event.status,
        attempts: event.attempts,
        nextAttemptAt: null,
        lastError: event.lastError,
      });
    }

    if (path === "/api/v1/admin/ops/reconciliation/runs" && method === "GET") {
      return json(route, state.runs);
    }

    if (path === "/api/v1/admin/ops/reconciliation/runs" && method === "POST") {
      counters.runReconciliationCalls += 1;
      const newRun: ReconciliationRun = {
        runId: `run-manual-00${counters.runReconciliationCalls}`,
        triggerType: "MANUAL",
        status: "COMPLETED",
        scannedSkuCount: 4,
        scannedSnapshotCount: 8,
        openDriftCount: state.drifts.length,
        failureMessage: null,
        createdAt: "2026-03-16T07:15:00Z",
        completedAt: "2026-03-16T07:15:05Z",
      };
      state.runs.unshift(newRun);
      return json(route, newRun);
    }

    if (path === "/api/v1/admin/ops/reconciliation/drifts" && method === "GET") {
      return json(route, state.drifts);
    }

    if (path.startsWith("/api/v1/admin/ops/reconciliation/") && path.endsWith("/resolve") && method === "POST") {
      counters.resolveDriftCalls += 1;
      const driftId = path.split("/")[6];
      const drift = state.drifts.find((item) => item.driftId === driftId);
      if (!drift) {
        return route.fulfill({ status: 404, body: "drift not found" });
      }

      const payload = request.postDataJSON() as { resolutionNote?: string } | null;
      drift.status = "RESOLVED";
      drift.resolutionNote = payload?.resolutionNote ?? null;
      drift.resolvedAt = "2026-03-16T07:16:00Z";
      state.drifts = state.drifts.filter((item) => item.driftId !== driftId);
      return json(route, drift);
    }

    if (path === "/api/v1/admin/ops/benchmarks/evidence" && method === "GET") {
      return json(route, [
        {
          runId: benchmarkRunId,
          timestamp: "2026-03-16T07:45:00Z",
          gitCommit: "abc1234",
          evidenceDir: "testing/k6/evidence/20260316-074500-abc1234",
          suiteStatus: "PASSED",
          businessChecksPassed: true,
          baselineTarget: "testing/k6/evidence/20260315-133859-e2e3644/report.json",
        },
      ]);
    }

    if (path === `/api/v1/admin/ops/benchmarks/evidence/${benchmarkRunId}` && method === "GET") {
      return json(route, buildBenchmarkDetail(benchmarkRunId));
    }

    if (path === "/api/v1/admin/ops/copilot/capabilities" && method === "GET") {
      return json(route, {
        enabled: true,
        provider: "Gemini",
        model: "gemini-ops-1",
        scopes: ["alerts", "benchmarks", "channels"],
      });
    }

    if (path === "/api/v1/admin/ops/copilot/analyze" && method === "POST") {
      return json(route, {
        summary: "Ops Copilot sees a backlog-driven drift on SHOPEE.",
        findings: [
          {
            title: "Pending outbox events",
            detail: "2 failed events are still pending retry after 5m.",
            severity: "WARN",
          },
        ],
        recommendedActions: [
          {
            label: "Open ops remediation",
            href: "/ops/remediation",
            detail: "Retry failed events and resolve drifts.",
          },
        ],
        citations: ["outbox.backlog.pending=2", "drifts.TIKTOK_SHOP.OPEN=1"],
        providerModel: "gemini-ops-1",
      });
    }

    if (path === "/api/v1/admin/channels/health" && method === "GET") {
      return json(route, [
        {
          channel: "SHOPEE",
          status: "HEALTHY",
          connectorMode: "REAL",
          configValid: true,
          syncBacklogCount: 0,
          staleSnapshotCount: 0,
          openDriftCount: 0,
          lastReconciliationAt: "2026-03-16T07:00:00Z",
          latestIngressReceipt: null,
          latestReplay: null,
        },
      ]);
    }

    return route.fulfill({
      status: 404,
      contentType: "text/plain",
      body: `No E2E mock configured for ${method} ${path}`,
    });
  });

  return counters;
}

test.describe("Admin UI operator workflows", () => {
  test("login workflow signs in and opens campaign management", async ({ page }) => {
    const counters = await installApiMock(page, {
      refreshSession: null,
      loginSession: ADMIN_SESSION,
    });

    await page.goto("/login");
    await expect(page.getByRole("heading", { name: "Inventory Control Tower" })).toBeVisible();
    await page.getByRole("button", { name: "Sign in" }).click();
    await expect(page.getByRole("heading", { name: "Campaign management" })).toBeVisible();
    expect(counters.loginCalls).toBe(1);
  });

  test("refresh bootstrap restores session and loads protected content", async ({ page }) => {
    const counters = await installApiMock(page, {
      refreshSession: ADMIN_SESSION,
    });

    await page.goto("/ops");
    await expect(page.getByRole("heading", { name: /alerts, backlog, drift/i })).toBeVisible();
    await expect
      .poll(() => counters.refreshCalls, { message: "refresh endpoint should be called during bootstrap" })
      .toBeGreaterThan(0);
  });

  test("operator deep-link to campaign route redirects to ops with notice", async ({ page }) => {
    await installApiMock(page, {
      refreshSession: OPERATOR_SESSION,
    });

    await page.goto("/campaigns/campaign-demo-001");
    await expect(page.getByRole("heading", { name: /alerts, backlog, drift/i })).toBeVisible();
    await expect(page.getByText(/campaign management is limited to admin sessions/i)).toBeVisible();
  });

  test("ops remediation supports outbox retry, reconciliation trigger, and drift resolve", async ({ page }) => {
    const counters = await installApiMock(page, {
      refreshSession: ADMIN_SESSION,
    });

    await page.goto("/ops/remediation");
    await expect(page.getByRole("heading", { name: "Ops remediation" })).toBeVisible();

    await page.getByRole("button", { name: "Retry" }).click();
    await expect(page.getByText(/retry queued for evt-failed-001/i)).toBeVisible();
    expect(counters.retryCalls).toBe(1);

    await page.getByRole("button", { name: "Run reconciliation" }).click();
    await expect(page.getByText(/reconciliation run triggered/i)).toBeVisible();
    expect(counters.runReconciliationCalls).toBe(1);

    await page.getByRole("tab", { name: "Drift detail" }).click();
    await page.getByLabel("Resolution note").fill("Resolved after manual reconciliation run.");
    await page.getByRole("button", { name: "Resolve drift" }).click();
    await expect(page.getByText(/drift drift-001 resolved/i)).toBeVisible();
    await expect(page.getByText(/no open drifts are waiting for resolution/i)).toBeVisible();
    expect(counters.resolveDriftCalls).toBe(1);
  });

  test("benchmark overview drills into benchmark detail", async ({ page }) => {
    await installApiMock(page, {
      refreshSession: ADMIN_SESSION,
    });

    await page.goto("/benchmarks");
    await expect(page.getByRole("heading", { name: "Benchmark reporting" })).toBeVisible();
    await page.getByRole("link", { name: /run-123/i }).click();
    await expect(page.getByRole("heading", { name: "Benchmark detail" })).toBeVisible();
    await expect(page.getByText("run-123").first()).toBeVisible();
    await expect(page.getByText("mixed-channel-flow").first()).toBeVisible();
  });

  test("channel-health route renders operator posture data", async ({ page }) => {
    await installApiMock(page, {
      refreshSession: OPERATOR_SESSION,
    });

    await page.goto("/channels/health");
    await expect(page.getByRole("heading", { name: "Channel health" })).toBeVisible();
    await expect(page.getByText("SHOPEE")).toBeVisible();
    await expect(page.getByRole("link", { name: /open remediation/i })).toBeVisible();
  });

  test("Ops Copilot panel surfaces Gemini findings", async ({ page }) => {
    await installApiMock(page, {
      refreshSession: ADMIN_SESSION,
    });

    await page.goto("/ops");
    await expect(page.getByRole("heading", { name: "Ops Copilot" })).toBeVisible();
    await page.getByRole("button", { name: /run ops copilot/i }).click();
    await expect(page.getByText(/backlog-driven drift on SHOPEE/i)).toBeVisible();
    await expect(page.getByRole("link", { name: /open ops remediation/i }).first()).toHaveAttribute(
      "href",
      "/ops/remediation",
    );
  });
});
