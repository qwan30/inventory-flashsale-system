import { act, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, vi } from "vitest";
import { OpsCopilotCapabilities } from "../../lib/api";
import { OpsPage } from "./OpsPage";
import { jsonResponse } from "../../test/mockApi";

type SessionRole = "ADMIN" | "OPERATOR";

const authState = vi.hoisted(() => ({
  session: {
    accessToken: "test-access-token",
    accessTokenExpiresAt: "2026-03-16T08:00:00Z",
    username: "admin",
    displayName: "System Admin",
    role: "ADMIN" as SessionRole,
  },
}));

vi.mock("../../state/auth", () => ({
  useAuth: () => ({
    session: authState.session,
    login: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
  }),
}));

const defaultAnalysis = {
  summary: "Ops Copilot sees a skew between alerts and drifts.",
  findings: [
    { title: "Backlog pressure", detail: "Failed outbox events stay pending for over 5 minutes.", severity: "WARN" },
  ],
  recommendedActions: [
    {
      label: "Inspect outbox backlog",
      href: "/ops/remediation",
      detail: "Retry failed events and re-run reconciliation.",
    },
  ],
  citations: ["alerts > OUTBOX_FAILED_BACKLOG", "drifts > TIKTOK_SHOP"],
  providerModel: "gemini-ops-1",
};

interface OpsMockOptions {
  capabilities?: Partial<OpsCopilotCapabilities>;
  analysis?: typeof defaultAnalysis;
}

function mockOpsFetch(options: OpsMockOptions = {}) {
  const capabilitiesPayload = {
    enabled: true,
    provider: "Gemini",
    model: "gemini-ops-1",
    scopes: ["alerts", "benchmarks", "channels"],
    ...options.capabilities,
  };
  const analysisPayload = options.analysis ?? defaultAnalysis;

  vi.stubGlobal("fetch", vi.fn(async (input, init) => {
    const url = typeof input === "string" ? input : input.url;
    const path = new URL(url).pathname;
    if (path === "/api/v1/admin/ops/alerts") {
      return jsonResponse([
        {
          code: "OUTBOX_FAILED_BACKLOG",
          severity: "WARN",
          status: "ACTIVE",
          message: "backlog high",
          currentValue: "12",
          threshold: "10",
          observedAt: "2026-03-16T07:10:00Z",
        },
      ]);
    }
    if (path === "/api/v1/admin/ops/outbox/backlog") {
      return jsonResponse({
        pendingCount: 4,
        failedCount: 2,
        retryableFailedCount: 1,
      });
    }
    if (path === "/api/v1/admin/ops/reconciliation/drifts") {
      return jsonResponse([
        {
          driftId: "drift-1",
          runId: "run-1",
          channel: "SHOPEE",
          sku: "SKU-DEMO-001",
          centralInventory: { availableQty: 10, reservedQty: 2, soldQty: 1 },
          observedInventory: { availableQty: 9, reservedQty: 2, soldQty: 1 },
          status: "OPEN",
          resolutionNote: null,
          resolvedAt: null,
        },
      ]);
    }
    if (path === "/api/v1/admin/ops/copilot/capabilities") {
      return jsonResponse(capabilitiesPayload);
    }
    if (path === "/api/v1/admin/ops/copilot/analyze" && init?.method === "POST") {
      return jsonResponse(analysisPayload);
    }
    return jsonResponse({});
  }));
}

describe("OpsPage", () => {
  beforeEach(() => {
    authState.session = {
      accessToken: "test-access-token",
      accessTokenExpiresAt: "2026-03-16T08:00:00Z",
      username: "admin",
      displayName: "System Admin",
      role: "ADMIN" as SessionRole,
    };
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows channel-health and remediation workflow links", async () => {
    mockOpsFetch();

    render(
      <MemoryRouter>
        <OpsPage />
      </MemoryRouter>,
    );

    expect(await screen.findByRole("link", { name: /^channel health$/i })).toHaveAttribute(
      "href",
      "/channels/health",
    );
    expect(screen.getByRole("link", { name: /open remediation/i })).toHaveAttribute(
      "href",
      "/ops/remediation",
    );
  });

  it("keeps channel-health workflow visible for operator sessions", async () => {
    authState.session = {
      ...authState.session,
      username: "operator",
      displayName: "Operations User",
      role: "OPERATOR" as SessionRole,
    };
    mockOpsFetch();

    render(
      <MemoryRouter
        initialEntries={[
          { pathname: "/ops", state: { notice: "Campaign management is limited to admin sessions." } },
        ]}
      >
        <OpsPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText(/campaign management is limited to admin sessions/i)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /^channel health$/i })).toHaveAttribute(
      "href",
      "/channels/health",
    );
  });

  it("runs Ops Copilot analysis and renders findings", async () => {
    mockOpsFetch();

    render(
      <MemoryRouter>
        <OpsPage />
      </MemoryRouter>,
    );

    await screen.findByRole("heading", { name: "Ops Copilot" });
    const runButton = await screen.findByRole("button", { name: /run ops copilot/i });
    await act(async () => {
      runButton.click();
    });

    expect(await screen.findByText(/Ops Copilot sees a skew between alerts and drifts/i)).toBeVisible();
    expect(screen.getByRole("link", { name: /inspect outbox backlog/i })).toHaveAttribute(
      "href",
      "/ops/remediation",
    );
  });

  it("shows disabled message when capabilities are off", async () => {
    mockOpsFetch({ capabilities: { enabled: false, message: "API key missing" } });

    render(
      <MemoryRouter>
        <OpsPage />
      </MemoryRouter>,
    );

    await screen.findByRole("heading", { name: "Ops Copilot" });
    expect(await screen.findByText(/API key missing/i)).toBeVisible();
    expect(screen.getByRole("button", { name: /run ops copilot/i })).toBeDisabled();
  });
});
