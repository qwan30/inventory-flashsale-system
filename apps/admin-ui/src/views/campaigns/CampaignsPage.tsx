import { Link } from "react-router-dom";
import { Panel } from "../../components/Panel";
import { EmptyState, InlineError, InlineLoading } from "../../components/ResourceState";
import { StatusBadge } from "../../components/StatusBadge";
import { useAuth } from "../../state/auth";
import { useAsyncResource } from "../../hooks/useAsyncResource";
import { fetchCampaigns } from "../../lib/api";
import { formatDateTime, formatNumber } from "../../lib/format";

export function CampaignsPage() {
  const { session } = useAuth();
  const resource = useAsyncResource(
    session ? () => fetchCampaigns(session.accessToken) : null,
    [session?.accessToken],
  );

  const campaigns = resource.data ?? [];
  const totals = {
    active: campaigns.filter((campaign) => campaign.status === "ACTIVE").length,
    draft: campaigns.filter((campaign) => campaign.status === "DRAFT").length,
    quota: campaigns.reduce((sum, campaign) => sum + campaign.quota, 0),
  };

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
      <Panel
        title="Campaign ledger"
        subtitle="Open a campaign to inspect lifecycle state, edit drafts, or review immutable audit history."
      >
        {resource.loading ? <InlineLoading message="Loading campaigns..." /> : null}
        {resource.error ? <InlineError message={resource.error} /> : null}
        {!resource.loading && !resource.error && campaigns.length === 0 ? (
          <EmptyState message="No campaigns are available yet." />
        ) : null}
        {campaigns.length > 0 ? (
          <div className="table-grid">
            <div className="table-row table-row-head">
              <div>Campaign</div>
              <div>Status</div>
              <div>Window</div>
              <div>Quota</div>
              <div>Reserved</div>
              <div>Sold</div>
            </div>
            {campaigns.map((campaign) => (
              <Link
                key={campaign.id}
                className="table-row table-row-link"
                to={`/campaigns/${campaign.id}`}
              >
                <div>
                  <strong>{campaign.id}</strong>
                  <p className="muted">{campaign.sku}</p>
                </div>
                <div>
                  <StatusBadge value={campaign.status} />
                </div>
                <div>
                  <strong>{formatDateTime(campaign.startsAt)}</strong>
                  <p className="muted">Ends {formatDateTime(campaign.endsAt)}</p>
                </div>
                <div>{formatNumber(campaign.quota)}</div>
                <div>{formatNumber(campaign.reservedQuota)}</div>
                <div>{formatNumber(campaign.soldQuota)}</div>
              </Link>
            ))}
          </div>
        ) : null}
      </Panel>
    </div>
  );
}
