import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";
import { AuthProvider } from "./state/auth";
import { TEST_SESSION, jsonResponse } from "./test/mockApi";

const OPERATOR_SESSION = {
  ...TEST_SESSION,
  username: "operator",
  displayName: "Operations User",
  role: "OPERATOR" as const,
};

function renderApp({
  initialEntries,
  initialSession,
  skipBootstrap,
}: {
  initialEntries: string[];
  initialSession?: typeof TEST_SESSION | typeof OPERATOR_SESSION | null;
  skipBootstrap?: boolean;
}) {
  render(
    <MemoryRouter initialEntries={initialEntries}>
      <AuthProvider initialSession={initialSession ?? null} skipBootstrap={skipBootstrap}>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
}

function stubProtectedPageFetch() {
  vi.stubGlobal(
    "fetch",
    vi.fn((input: string | URL | Request) => {
      const url = typeof input === "string" ? input : input instanceof URL ? input.toString() : input.url;

      if (url.endsWith("/api/v1/admin/campaigns")) {
        return jsonResponse([]);
      }

      if (url.endsWith("/api/v1/admin/ops/alerts")) {
        return jsonResponse([]);
      }

      if (url.endsWith("/api/v1/admin/ops/outbox/backlog")) {
        return jsonResponse({
          pendingCount: 0,
          failedCount: 0,
          retryableFailedCount: 0,
        });
      }

      if (url.endsWith("/api/v1/admin/ops/reconciliation/drifts")) {
        return jsonResponse([]);
      }

      if (url.endsWith("/api/v1/admin/channels/health")) {
        return jsonResponse([]);
      }

      return Promise.reject(new Error(`Unexpected fetch call in App.test.tsx: ${url}`));
    }),
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("App", () => {
  it("renders the login page when unauthenticated on /login", () => {
    renderApp({
      initialEntries: ["/login"],
      skipBootstrap: true,
    });

    expect(screen.getByRole("heading", { name: /inventory control tower/i })).toBeInTheDocument();
  });

  it("shows a restoring-session state while protected-route bootstrap is pending", () => {
    vi.stubGlobal("fetch", vi.fn(() => new Promise(() => undefined)));

    renderApp({
      initialEntries: ["/ops"],
    });

    expect(screen.getByRole("heading", { name: /restoring session/i })).toBeInTheDocument();
  });

  it("redirects operator campaign deep links to ops and hides the campaigns nav item", async () => {
    stubProtectedPageFetch();

    renderApp({
      initialEntries: ["/campaigns/campaign-demo-001"],
      initialSession: OPERATOR_SESSION,
      skipBootstrap: true,
    });

    expect(await screen.findByRole("heading", { name: /alerts, backlog, drift/i })).toBeInTheDocument();
    expect(screen.getByText(/campaign management is limited to admin sessions/i)).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /campaigns/i })).not.toBeInTheDocument();
  });

  it("routes admins to campaign management from the root route", async () => {
    stubProtectedPageFetch();

    renderApp({
      initialEntries: ["/"],
      initialSession: TEST_SESSION,
      skipBootstrap: true,
    });

    expect(await screen.findByRole("heading", { name: /campaign management/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /campaigns/i })).toBeInTheDocument();
  });

  it("allows operator sessions to open channel health", async () => {
    stubProtectedPageFetch();

    renderApp({
      initialEntries: ["/channels/health"],
      initialSession: OPERATOR_SESSION,
      skipBootstrap: true,
    });

    expect(await screen.findByRole("heading", { name: /channel health/i })).toBeInTheDocument();
  });
});
