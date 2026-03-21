import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import { OpsRemediationPage } from "./OpsRemediationPage";
import { jsonResponse } from "../../test/mockApi";

const authState = vi.hoisted(() => ({
  session: {
    accessToken: "test-access-token",
    accessTokenExpiresAt: "2026-03-16T08:00:00Z",
    username: "admin",
    displayName: "System Admin",
    role: "ADMIN" as const,
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

describe("OpsRemediationPage", () => {
  it("retries a failed outbox event from the default tab", async () => {
    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() =>
        jsonResponse([
          {
            eventId: "evt-1",
            aggregateType: "reservation",
            aggregateId: "res-1",
            eventType: "inventory.reservation.created",
            eventVersion: 1,
            status: "FAILED",
            attempts: 3,
            lastError: "broker down",
            nextAttemptAt: null,
            createdAt: "2026-03-16T07:00:00Z",
            updatedAt: "2026-03-16T07:10:00Z",
          },
        ]),
      )
      .mockImplementationOnce(() =>
        jsonResponse([
          {
            runId: "run-1",
            triggerType: "MANUAL",
            status: "COMPLETED",
            scannedSkuCount: 1,
            scannedSnapshotCount: 2,
            openDriftCount: 1,
            failureMessage: null,
            createdAt: "2026-03-16T07:00:00Z",
            completedAt: "2026-03-16T07:01:00Z",
          },
        ]),
      )
      .mockImplementationOnce(() =>
        jsonResponse([
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
        ]),
      )
      .mockImplementationOnce(() =>
        jsonResponse({
          eventId: "evt-1",
          status: "PENDING",
          attempts: 4,
          nextAttemptAt: null,
          lastError: null,
        }),
      )
      .mockImplementationOnce(() =>
        jsonResponse([
          {
            eventId: "evt-1",
            aggregateType: "reservation",
            aggregateId: "res-1",
            eventType: "inventory.reservation.created",
            eventVersion: 1,
            status: "PENDING",
            attempts: 4,
            lastError: null,
            nextAttemptAt: null,
            createdAt: "2026-03-16T07:00:00Z",
            updatedAt: "2026-03-16T07:12:00Z",
          },
        ]),
      );
    vi.stubGlobal("fetch", fetchMock);

    render(
      <MemoryRouter>
        <OpsRemediationPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText("evt-1")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /retry/i }));

    expect(await screen.findByText(/retry queued for evt-1/i)).toBeInTheDocument();
    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/v1/admin/ops/outbox/evt-1/retry",
        expect.objectContaining({ method: "POST" }),
      ),
    );
  });

  it("switches to the reconciliation runs tab", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockImplementationOnce(() => jsonResponse([]))
        .mockImplementationOnce(() =>
          jsonResponse([
            {
              runId: "run-1",
              triggerType: "MANUAL",
              status: "COMPLETED",
              scannedSkuCount: 4,
              scannedSnapshotCount: 8,
              openDriftCount: 1,
              failureMessage: null,
              createdAt: "2026-03-16T07:00:00Z",
              completedAt: "2026-03-16T07:01:00Z",
            },
          ]),
        )
        .mockImplementationOnce(() => jsonResponse([])),
    );

    render(
      <MemoryRouter>
        <OpsRemediationPage />
      </MemoryRouter>,
    );

    fireEvent.click(await screen.findByRole("tab", { name: /reconciliation runs/i }));

    expect(await screen.findByText("run-1")).toBeInTheDocument();
    expect(screen.getByText(/4 skus/i)).toBeInTheDocument();
  });

  it("triggers a reconciliation run with inline feedback", async () => {
    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() => jsonResponse([]))
      .mockImplementationOnce(() => jsonResponse([]))
      .mockImplementationOnce(() => jsonResponse([]))
      .mockImplementationOnce(() =>
        jsonResponse({
          runId: "run-2",
          triggerType: "MANUAL",
          status: "RUNNING",
          scannedSkuCount: 0,
          scannedSnapshotCount: 0,
          openDriftCount: 0,
          failureMessage: null,
          createdAt: "2026-03-16T07:05:00Z",
          completedAt: null,
        }),
      )
      .mockImplementationOnce(() =>
        jsonResponse([
          {
            runId: "run-2",
            triggerType: "MANUAL",
            status: "RUNNING",
            scannedSkuCount: 0,
            scannedSnapshotCount: 0,
            openDriftCount: 0,
            failureMessage: null,
            createdAt: "2026-03-16T07:05:00Z",
            completedAt: null,
          },
        ]),
      )
      .mockImplementationOnce(() => jsonResponse([]));
    vi.stubGlobal("fetch", fetchMock);

    render(
      <MemoryRouter>
        <OpsRemediationPage />
      </MemoryRouter>,
    );

    fireEvent.click(await screen.findByRole("button", { name: /run reconciliation/i }));

    expect(await screen.findByText(/reconciliation run triggered/i)).toBeInTheDocument();
    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/v1/admin/ops/reconciliation/runs",
        expect.objectContaining({ method: "POST" }),
      ),
    );
  });

  it("resolves a drift with a note and reloads data", async () => {
    let driftFetchCount = 0;
    let runFetchCount = 0;
    const fetchMock = vi.fn((input: string | URL | Request, init?: RequestInit) => {
      const url = typeof input === "string" ? input : input instanceof URL ? input.toString() : input.url;
      const method = init?.method ?? "GET";

      if (url.endsWith("/api/v1/admin/ops/outbox/events")) {
        return jsonResponse([]);
      }

      if (url.endsWith("/api/v1/admin/ops/reconciliation/runs") && method === "GET") {
        runFetchCount += 1;
        return jsonResponse([
          {
            runId: "run-1",
            triggerType: "MANUAL",
            status: "COMPLETED",
            scannedSkuCount: 4,
            scannedSnapshotCount: 8,
            openDriftCount: runFetchCount > 1 ? 0 : 1,
            failureMessage: null,
            createdAt: "2026-03-16T07:00:00Z",
            completedAt: "2026-03-16T07:01:00Z",
          },
        ]);
      }

      if (url.endsWith("/api/v1/admin/ops/reconciliation/drifts") && method === "GET") {
        driftFetchCount += 1;
        if (driftFetchCount > 1) {
          return jsonResponse([]);
        }

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

      if (url.endsWith("/api/v1/admin/ops/reconciliation/drift-1/resolve") && method === "POST") {
        return jsonResponse({
          driftId: "drift-1",
          runId: "run-1",
          channel: "SHOPEE",
          sku: "SKU-DEMO-001",
          centralInventory: { availableQty: 10, reservedQty: 2, soldQty: 1 },
          observedInventory: { availableQty: 9, reservedQty: 2, soldQty: 1 },
          status: "RESOLVED",
          resolutionNote: "Confirmed channel snapshot lag.",
          resolvedAt: "2026-03-16T07:03:00Z",
        });
      }

      return Promise.reject(new Error(`Unexpected fetch call: ${method} ${url}`));
    });
    vi.stubGlobal("fetch", fetchMock);

    render(
      <MemoryRouter>
        <OpsRemediationPage />
      </MemoryRouter>,
    );

    fireEvent.click(await screen.findByRole("tab", { name: /drift detail/i }));
    fireEvent.change(await screen.findByLabelText(/resolution note/i), {
      target: { value: "Confirmed channel snapshot lag." },
    });
    fireEvent.click(screen.getByRole("button", { name: /resolve drift/i }));

    expect(await screen.findByText(/drift drift-1 resolved/i)).toBeInTheDocument();
    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/v1/admin/ops/reconciliation/drift-1/resolve",
        expect.objectContaining({ method: "POST" }),
      ),
    );
  });
});
