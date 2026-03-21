import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { vi } from "vitest";
import { CampaignAuditPage } from "./CampaignAuditPage";
import { CampaignDetailPage } from "./CampaignDetailPage";
import { CampaignsPage } from "./CampaignsPage";
import { jsonResponse, TEST_SESSION } from "../../test/mockApi";

vi.mock("../../state/auth", () => ({
  useAuth: () => ({
    session: TEST_SESSION,
    login: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
  }),
}));

function renderRoute(path: string) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/campaigns" element={<CampaignsPage />} />
        <Route path="/campaigns/:campaignId" element={<CampaignDetailPage />} />
        <Route path="/campaigns/:campaignId/audits" element={<CampaignAuditPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("campaign routes", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders campaign overview rows as launcher links", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation(() =>
      jsonResponse([
        {
          id: "campaign-demo-001",
          sku: "SKU-DEMO-001",
          startsAt: "2026-03-16T08:00:00Z",
          endsAt: "2026-03-16T10:00:00Z",
          quota: 25,
          reservedQuota: 5,
          soldQuota: 2,
          status: "ACTIVE",
          createdAt: "2026-03-16T07:00:00Z",
          updatedAt: "2026-03-16T07:30:00Z",
        },
      ]),
    ));

    renderRoute("/campaigns");

    expect(await screen.findByRole("link", { name: /campaign-demo-001/i })).toHaveAttribute(
      "href",
      "/campaigns/campaign-demo-001",
    );
  });

  it("renders state-aware actions for draft campaigns", async () => {
    let callIndex = 0;
    const initialResponse = {
      id: "campaign-demo-001",
      sku: "SKU-DEMO-001",
      startsAt: "2026-03-16T08:00:00Z",
      endsAt: "2026-03-16T10:00:00Z",
      quota: 25,
      reservedQuota: 5,
      soldQuota: 2,
      status: "DRAFT",
      createdAt: "2026-03-16T07:00:00Z",
      updatedAt: "2026-03-16T07:30:00Z",
    };
    const updatedResponse = {
      ...initialResponse,
      quota: 40,
      updatedAt: "2026-03-16T07:45:00Z",
    };
    const fetchMock = vi.fn(async () => {
      const response = callIndex === 0 ? initialResponse : updatedResponse;
      callIndex += 1;
      return jsonResponse(response);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderRoute("/campaigns/campaign-demo-001");

    expect(await screen.findByRole("heading", { name: /campaign detail/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /save draft changes/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /activate campaign/i })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /end campaign/i })).not.toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/quota/i), { target: { value: "40" } });
    fireEvent.click(screen.getByRole("button", { name: /save draft changes/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
  });

  it("renders audit summary and immutable activity rows", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockImplementationOnce(() =>
          jsonResponse({
            id: "campaign-demo-001",
            sku: "SKU-DEMO-001",
            startsAt: "2026-03-16T08:00:00Z",
            endsAt: "2026-03-16T10:00:00Z",
            quota: 25,
            reservedQuota: 5,
            soldQuota: 2,
            status: "ACTIVE",
            createdAt: "2026-03-16T07:00:00Z",
            updatedAt: "2026-03-16T07:30:00Z",
          }),
        )
        .mockImplementationOnce(() =>
          jsonResponse([
            {
              actorUsername: "admin",
              actorRole: "ADMIN",
              action: "CAMPAIGN_UPDATED",
              resourceType: "CAMPAIGN",
              resourceId: "campaign-demo-001",
              outcome: "SUCCESS",
              correlationId: "corr-1",
              details: "quota=25",
              createdAt: "2026-03-16T07:10:00Z",
            },
            {
              actorUsername: "admin",
              actorRole: "ADMIN",
              action: "CAMPAIGN_ACTIVATED",
              resourceType: "CAMPAIGN",
              resourceId: "campaign-demo-001",
              outcome: "FAILURE",
              correlationId: "corr-2",
              details: "validation failed",
              createdAt: "2026-03-16T07:15:00Z",
            },
          ]),
        ),
    );

    renderRoute("/campaigns/campaign-demo-001/audits");

    const totalActionsLabel = await screen.findByText("Total actions");
    expect(totalActionsLabel.parentElement?.querySelector("strong")).toHaveTextContent("2");
    expect(screen.getByText("corr-2")).toBeInTheDocument();
    expect(screen.getByText(/validation failed/i)).toBeInTheDocument();
  });

  it("renders ended campaigns as read-only", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(() =>
        jsonResponse({
          id: "campaign-demo-001",
          sku: "SKU-DEMO-001",
          startsAt: "2026-03-16T08:00:00Z",
          endsAt: "2026-03-16T10:00:00Z",
          quota: 25,
          reservedQuota: 5,
          soldQuota: 2,
          status: "ENDED",
          createdAt: "2026-03-16T07:00:00Z",
          updatedAt: "2026-03-16T07:30:00Z",
        }),
      ),
    );

    renderRoute("/campaigns/campaign-demo-001");

    expect(await screen.findByText(/can no longer be edited/i)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /save draft changes/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /activate campaign/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /end campaign/i })).not.toBeInTheDocument();
  });
});
