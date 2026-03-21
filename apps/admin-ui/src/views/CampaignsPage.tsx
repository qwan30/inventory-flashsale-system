import { useMemo } from "react";
import { Panel } from "../components/Panel";
import { useAuth } from "../state/auth";
import { useAsyncResource } from "../hooks/useAsyncResource";
import { fetchCampaigns } from "../lib/api";

export function CampaignsPage() {
  const { session } = useAuth();
  const resource = useAsyncResource(
    session ? () => fetchCampaigns(session.accessToken) : null,
    [session?.accessToken],
  );

  const totals = useMemo(() => {
    const campaigns = resource.data ?? [];
    return {
      active: campaigns.filter((campaign) => campaign.status === "ACTIVE").length,
      draft: campaigns.filter((campaign) => campaign.status === "DRAFT").length,
      quota: campaigns.reduce((sum, campaign) => sum + campaign.quota, 0),
    };
  }, [resource.data]);

  return (
    <div className="stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Campaign operations</p>
          <h1>Campaign management</h1>
        </div>
        <div className="stats-row">
          <div className="stat-card">
            <strong>{totals.active}</strong>
            <span>Active</span>
          </div>
          <div className="stat-card">
            <strong>{totals.draft}</strong>
            <span>Draft</span>
          </div>
          <div className="stat-card">
            <strong>{totals.quota}</strong>
            <span>Total quota</span>
          </div>
        </div>
      </header>
      <Panel title="Campaign ledger" subtitle="Current admin API surface is wired for list and lifecycle actions.">
        {resource.loading ? <p>Loading campaigns...</p> : null}
        {resource.error ? <p className="error-banner">{resource.error}</p> : null}
        <div className="table-grid">
          {(resource.data ?? []).map((campaign) => (
            <article key={campaign.id} className="table-row">
              <div>
                <strong>{campaign.id}</strong>
                <p className="muted">{campaign.sku}</p>
              </div>
              <div>{campaign.status}</div>
              <div>{campaign.quota}</div>
              <div>{campaign.reservedQuota}</div>
              <div>{campaign.soldQuota}</div>
            </article>
          ))}
        </div>
      </Panel>
    </div>
  );
}
