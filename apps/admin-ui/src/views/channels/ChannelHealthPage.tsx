import { useMemo } from "react";
import { Link } from "react-router-dom";
import { Panel } from "../../components/Panel";
import { EmptyState, InlineError, InlineLoading } from "../../components/ResourceState";
import { StatusBadge } from "../../components/StatusBadge";
import { useAsyncResource } from "../../hooks/useAsyncResource";
import { fetchChannelHealth, type ChannelHealthSummary } from "../../lib/api";
import { formatDateTime, formatNumber } from "../../lib/format";
import { useAuth } from "../../state/auth";

export function ChannelHealthPage() {
  const { session } = useAuth();
  const channelHealth = useAsyncResource(
    session ? () => fetchChannelHealth(session.accessToken) : null,
    [session?.accessToken, "channel-health"],
  );
  const channelSummaries = useMemo(
    () => (channelHealth.data ?? []).slice().sort((left, right) => left.channel.localeCompare(right.channel)),
    [channelHealth.data],
  );

  const summary = useMemo(() => {
    const healthy = channelSummaries.filter((item) => item.status === "HEALTHY").length;
    const degraded = channelSummaries.filter((item) => item.status === "DEGRADED").length;
    const unavailable = channelSummaries.filter((item) => item.status === "UNAVAILABLE").length;
    const totalOpenDrifts = channelSummaries.reduce((total, item) => total + item.openDriftCount, 0);
    const totalBacklog = channelSummaries.reduce((total, item) => total + item.syncBacklogCount, 0);

    return {
      healthy,
      degraded,
      unavailable,
      totalOpenDrifts,
      totalBacklog,
    };
  }, [channelSummaries]);

  return (
    <div className="stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Channel posture</p>
          <h1>Channel health</h1>
          <p className="muted">
            Marketplace posture view built from the dedicated admin channel-health contract.
          </p>
        </div>
        <div className="page-actions">
          <Link className="text-link" to="/ops">
            Back to ops
          </Link>
          <Link className="text-link" to="/ops/remediation">
            Open remediation
          </Link>
        </div>
      </header>
      {channelHealth.loading ? (
        <InlineLoading message="Loading channel posture..." />
      ) : null}
      {channelHealth.error ? <InlineError message={channelHealth.error} /> : null}
      <Panel
        title="Posture summary"
        subtitle="Summary counts across marketplace channels only."
      >
        <div className="summary-grid">
          <div className="summary-item">
            <span className="muted">Healthy channels</span>
            <strong>{formatNumber(summary.healthy)}</strong>
          </div>
          <div className="summary-item">
            <span className="muted">Degraded channels</span>
            <strong>{formatNumber(summary.degraded)}</strong>
          </div>
          <div className="summary-item">
            <span className="muted">Unavailable channels</span>
            <strong>{formatNumber(summary.unavailable)}</strong>
          </div>
          <div className="summary-item">
            <span className="muted">Total open drifts</span>
            <strong>{formatNumber(summary.totalOpenDrifts)}</strong>
          </div>
          <div className="summary-item">
            <span className="muted">Total sync backlog</span>
            <strong>{formatNumber(summary.totalBacklog)}</strong>
          </div>
        </div>
      </Panel>
      <Panel title="Channel cards" subtitle="Per-channel connector posture, drift, ingress, and replay signals.">
        {!channelHealth.loading && !channelHealth.error && channelSummaries.length === 0 ? (
          <EmptyState message="No channel posture data is available yet." />
        ) : (
          <div className="channel-grid">
            {channelSummaries.map((summary) => (
              <article key={summary.channel} className="channel-card">
                <div className="summary-inline">
                  <strong>{summary.channel}</strong>
                  <StatusBadge value={summary.status} />
                </div>
                <div className="summary-grid">
                  <div className="summary-item">
                    <span className="muted">Connector mode</span>
                    <strong>{summary.connectorMode}</strong>
                  </div>
                  <div className="summary-item">
                    <span className="muted">Config state</span>
                    <strong>{summary.configValid ? "Valid" : "Missing"}</strong>
                  </div>
                  <div className="summary-item">
                    <span className="muted">Sync backlog</span>
                    <strong>{formatNumber(summary.syncBacklogCount)}</strong>
                  </div>
                  <div className="summary-item">
                    <span className="muted">Stale snapshots</span>
                    <strong>{formatNumber(summary.staleSnapshotCount)}</strong>
                  </div>
                  <div className="summary-item">
                    <span className="muted">Open drifts</span>
                    <strong>{formatNumber(summary.openDriftCount)}</strong>
                  </div>
                  <div className="summary-item">
                    <span className="muted">Last reconciliation</span>
                    <strong>{formatDateTime(summary.lastReconciliationAt)}</strong>
                  </div>
                </div>
                <div>
                  <p className="muted">Latest ingress receipt</p>
                  {summary.latestIngressReceipt ? (
                    <div className="pill-row pill-row-stacked">
                      <StatusBadge value={summary.latestIngressReceipt.outcome} />
                      <span>{summary.latestIngressReceipt.type}</span>
                      <strong>{summary.latestIngressReceipt.externalReceiptId}</strong>
                      <span>{formatDateTime(summary.latestIngressReceipt.processedAt)}</span>
                    </div>
                  ) : (
                    <p className="muted">No ingress receipt observed.</p>
                  )}
                </div>
                <div>
                  <p className="muted">Latest replay</p>
                  {summary.latestReplay ? (
                    <div className="stack">
                      <div className="pill-row pill-row-stacked">
                        <StatusBadge value={summary.latestReplay.outcome} />
                        <span>{summary.latestReplay.action}</span>
                        <strong>{summary.latestReplay.resourceId}</strong>
                        <span>{formatDateTime(summary.latestReplay.createdAt)}</span>
                      </div>
                      <p className="muted">{summary.latestReplay.details}</p>
                    </div>
                  ) : (
                    <p className="muted">No replay action recorded.</p>
                  )}
                </div>
              </article>
            ))}
          </div>
        )}
      </Panel>
    </div>
  );
}
