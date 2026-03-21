import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { vi } from "vitest";
import { BenchmarkDetailPage } from "./BenchmarkDetailPage";
import { BenchmarksPage } from "./BenchmarksPage";
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

describe("benchmark pages", () => {
  it("renders promoted run links from the benchmark overview", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(() =>
        jsonResponse([
          {
            runId: "run-2026-03-16",
            timestamp: "2026-03-16T07:00:00Z",
            gitCommit: "abc123",
            evidenceDir: "/tmp/evidence",
            suiteStatus: "PASSED",
            businessChecksPassed: true,
            baselineTarget: "baseline-1",
          },
        ]),
      ),
    );

    render(
      <MemoryRouter>
        <BenchmarksPage />
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole("link", { name: /run-2026-03-16/i }),
    ).toHaveAttribute("href", "/benchmarks/run-2026-03-16");
  });

  it("renders typed benchmark summaries and baseline comparison data", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(() =>
        jsonResponse({
          entry: {
            runId: "run-2026-03-16",
            timestamp: "2026-03-16T07:00:00Z",
            gitCommit: "abc123",
            evidenceDir: "/tmp/evidence",
            suiteStatus: "PASSED",
            businessChecksPassed: true,
            baselineTarget: "baseline-1",
          },
          manifest: { files: [] },
          report: { status: "ok" },
          comparison: {},
          summaryMarkdown: "Everything looks good.",
          suiteSummary: {
            suiteStatus: "PASSED",
            businessChecksPassed: true,
            baselineTarget: "baseline-1",
            baselineAvailable: true,
            baselineNote: "Baseline available",
          },
          scenarioSummaries: [
            {
              name: "reserve",
              status: "PASSED",
              averageLatencyMs: 12.5,
              p95LatencyMs: 18.2,
              failedRate: 0,
              checksRate: 1,
              postRunChecks: ["latency budget"],
            },
          ],
          scenarioComparisons: [
            {
              scenarioName: "reserve",
              available: true,
              note: "Slight regression on latency.",
              delta: {
                deltaAverageLatencyMs: 1.2,
                deltaP95LatencyMs: 2.5,
                deltaFailedRate: 0,
                deltaChecksRate: 0,
              },
            },
          ],
        }),
      ),
    );

    render(
      <MemoryRouter initialEntries={["/benchmarks/run-2026-03-16"]}>
        <Routes>
          <Route path="/benchmarks/:runId" element={<BenchmarkDetailPage />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText(/slight regression on latency/i)).toBeInTheDocument();
    expect(screen.getByText(/avg \+1.20 ms/i)).toBeInTheDocument();
    expect(screen.getByText(/everything looks good/i)).toBeInTheDocument();
  });

  it("renders a no-baseline fallback when comparison data is unavailable", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(() =>
        jsonResponse({
          entry: {
            runId: "run-2026-03-16",
            timestamp: "2026-03-16T07:00:00Z",
            gitCommit: "abc123",
            evidenceDir: "/tmp/evidence",
            suiteStatus: "PASSED",
            businessChecksPassed: true,
            baselineTarget: null,
          },
          manifest: { files: [] },
          report: { status: "ok" },
          comparison: null,
          summaryMarkdown: null,
          suiteSummary: {
            suiteStatus: "PASSED",
            businessChecksPassed: true,
            baselineTarget: null,
            baselineAvailable: false,
            baselineNote: "No promoted baseline is available yet.",
          },
          scenarioSummaries: [],
          scenarioComparisons: [],
        }),
      ),
    );

    render(
      <MemoryRouter initialEntries={["/benchmarks/run-2026-03-16"]}>
        <Routes>
          <Route path="/benchmarks/:runId" element={<BenchmarkDetailPage />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText(/no promoted baseline is available yet/i)).toBeInTheDocument();
    expect(screen.getByText(/no baseline comparison data is available for this run/i)).toBeInTheDocument();
  });
});
