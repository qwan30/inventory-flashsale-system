import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach } from "vitest";
import { vi } from "vitest";
import { ChannelHealthPage } from "./ChannelHealthPage";
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

describe("ChannelHealthPage", () => {
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

  it("shows loading state while channel posture is pending", () => {
    vi.stubGlobal("fetch", vi.fn(() => new Promise(() => undefined)));

    render(
      <MemoryRouter>
        <ChannelHealthPage />
      </MemoryRouter>,
    );

    expect(screen.getByText(/loading channel posture/i)).toBeInTheDocument();
  });

  it("shows empty state when no channel summaries are returned", async () => {
    vi.stubGlobal("fetch", vi.fn(() => jsonResponse([])));

    render(
      <MemoryRouter>
        <ChannelHealthPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText(/no channel posture data is available yet/i)).toBeInTheDocument();
  });

  it("renders degraded posture details with ingress and replay summaries", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() =>
        jsonResponse([
          {
            channel: "TIKTOK_SHOP",
            status: "DEGRADED",
            connectorMode: "real",
            configValid: true,
            syncBacklogCount: 7,
            staleSnapshotCount: 2,
            openDriftCount: 3,
            lastReconciliationAt: "2026-03-16T07:00:00Z",
            latestIngressReceipt: {
              type: "ORDER_STATUS",
              externalReceiptId: "rcpt-1",
              outcome: "PROCESSED",
              processedAt: "2026-03-16T07:05:00Z",
            },
            latestReplay: {
              action: "TIKTOK_REPLAY_TRIGGERED",
              resourceId: "rcpt-1",
              outcome: "SUCCESS",
              createdAt: "2026-03-16T07:06:00Z",
              details: "manual replay",
            },
          },
        ]),
      ),
    );

    render(
      <MemoryRouter>
        <ChannelHealthPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText("TIKTOK_SHOP")).toBeInTheDocument();
    expect(screen.getByText("DEGRADED")).toBeInTheDocument();
    expect(screen.getByText("ORDER_STATUS")).toBeInTheDocument();
    expect(screen.getByText("ORDER_STATUS")).toBeInTheDocument();
    expect(screen.getAllByText("rcpt-1")).toHaveLength(2);
    expect(screen.getByText(/manual replay/i)).toBeInTheDocument();
  });

  it("renders mixed channel states including unavailable connectors", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() =>
        jsonResponse([
          {
            channel: "SHOPEE",
            status: "HEALTHY",
            connectorMode: "real",
            configValid: true,
            syncBacklogCount: 0,
            staleSnapshotCount: 0,
            openDriftCount: 0,
            lastReconciliationAt: "2026-03-16T07:00:00Z",
            latestIngressReceipt: null,
            latestReplay: null,
          },
          {
            channel: "TIKTOK_SHOP",
            status: "UNAVAILABLE",
            connectorMode: "real",
            configValid: false,
            syncBacklogCount: 0,
            staleSnapshotCount: 0,
            openDriftCount: 0,
            lastReconciliationAt: null,
            latestIngressReceipt: null,
            latestReplay: null,
          },
        ]),
      ),
    );

    render(
      <MemoryRouter>
        <ChannelHealthPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText("SHOPEE")).toBeInTheDocument();
    expect(screen.getByText("HEALTHY")).toBeInTheDocument();
    expect(screen.getByText("UNAVAILABLE")).toBeInTheDocument();
    expect(screen.getAllByText(/no replay action recorded/i).length).toBeGreaterThan(0);
  });

  it("renders for operator sessions with the same workflow links", async () => {
    authState.session = {
      ...authState.session,
      username: "operator",
      displayName: "Operations User",
      role: "OPERATOR" as SessionRole,
    };

    vi.stubGlobal(
      "fetch",
      vi.fn(() =>
        jsonResponse([
          {
            channel: "TIKTOK_SHOP",
            status: "HEALTHY",
            connectorMode: "mock",
            configValid: true,
            syncBacklogCount: 0,
            staleSnapshotCount: 0,
            openDriftCount: 0,
            lastReconciliationAt: null,
            latestIngressReceipt: null,
            latestReplay: null,
          },
        ]),
      ),
    );

    render(
      <MemoryRouter>
        <ChannelHealthPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText("TIKTOK_SHOP")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /open remediation/i })).toHaveAttribute("href", "/ops/remediation");
  });
});
