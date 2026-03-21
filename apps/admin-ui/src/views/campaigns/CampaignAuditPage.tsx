import { Link, useParams } from "react-router-dom";
import { Panel } from "../../components/Panel";
import { EmptyState, InlineError, InlineLoading } from "../../components/ResourceState";
import { StatusBadge } from "../../components/StatusBadge";
import { useAsyncResource } from "../../hooks/useAsyncResource";
import { fetchCampaign, fetchCampaignAudits } from "../../lib/api";
import { formatDateTime } from "../../lib/format";
import { useAuth } from "../../state/auth";

export function CampaignAuditPage() {
  const { campaignId } = useParams();
  const { session } = useAuth();
  const campaign = useAsyncResource(
    session && campaignId ? () => fetchCampaign(session.accessToken, campaignId) : null,
    [session?.accessToken, campaignId, "campaign"],
  );
  const audits = useAsyncResource(
    session && campaignId ? () => fetchCampaignAudits(session.accessToken, campaignId) : null,
    [session?.accessToken, campaignId, "audits"],
  );

  const entries = audits.data ?? [];
  const failedAttempts = entries.filter((entry) => entry.outcome !== "SUCCESS").length;
  const actorCount = new Set(entries.map((entry) => entry.actorUsername)).size;

  return (
    <div className="stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Campaign workflow</p>
          <h1>Campaign audit</h1>
          {campaign.data ? (
            <p className="muted">{campaign.data.id} • {campaign.data.sku}</p>
          ) : (
            <p className="muted">Campaign {campaignId}</p>
          )}
        </div>
        <div className="page-actions">
          <Link className="text-link" to={`/campaigns/${campaignId}`}>
            Back to detail
          </Link>
        </div>
      </header>
      {campaign.loading || audits.loading ? (
        <InlineLoading message="Loading campaign audit history..." />
      ) : null}
      {campaign.error ? <InlineError message={campaign.error} /> : null}
      {audits.error ? <InlineError message={audits.error} /> : null}
      {campaign.data ? (
        <Panel
          title={`${campaign.data.id} activity`}
          subtitle="Immutable activity log for campaign lifecycle changes."
        >
          <div className="summary-grid">
            <div className="summary-item">
              <span className="muted">Status</span>
              <div className="summary-inline">
                <StatusBadge value={campaign.data.status} />
              </div>
            </div>
            <div className="summary-item">
              <span className="muted">Total actions</span>
              <strong>{entries.length}</strong>
            </div>
            <div className="summary-item">
              <span className="muted">Failed attempts</span>
              <strong>{failedAttempts}</strong>
            </div>
            <div className="summary-item">
              <span className="muted">Unique actors</span>
              <strong>{actorCount}</strong>
            </div>
          </div>
        </Panel>
      ) : null}
      <Panel title="Audit table" subtitle="Primary surface for actor, outcome, and timestamp review.">
        {!audits.loading && !audits.error && entries.length === 0 ? (
          <EmptyState message="No audit entries exist for this campaign yet." />
        ) : null}
        {entries.length > 0 ? (
          <div className="table-grid">
            <div className="table-row table-row-head">
              <div>Action</div>
              <div>Actor</div>
              <div>Outcome</div>
              <div>Correlation ID</div>
              <div>Timestamp</div>
            </div>
            {entries.map((entry) => (
              <div key={`${entry.createdAt}-${entry.action}`} className="table-row table-row-wide">
                <div>
                  <strong>{entry.action}</strong>
                  <p className="muted">{entry.details}</p>
                </div>
                <div>
                  <strong>{entry.actorUsername}</strong>
                  <p className="muted">{entry.actorRole}</p>
                </div>
                <div>
                  <StatusBadge value={entry.outcome} />
                </div>
                <div className="mono-text">{entry.correlationId || "Not available"}</div>
                <div>{formatDateTime(entry.createdAt)}</div>
              </div>
            ))}
          </div>
        ) : null}
      </Panel>
    </div>
  );
}
