import { Link, useLocation } from "react-router-dom";
import { Panel } from "../../components/Panel";
import { EmptyState, InlineError, InlineLoading } from "../../components/ResourceState";
import { StatusBadge } from "../../components/StatusBadge";
import { useAuth } from "../../state/auth";
import { useAsyncResource } from "../../hooks/useAsyncResource";
import { formatDateTime } from "../../lib/format";
import { fetchAlerts, fetchBacklog, fetchDrifts } from "../../lib/api";
import { InlineBanner } from "../../components/InlineBanner";
import { OpsCopilotPanel } from "./OpsCopilotPanel";

export function OpsPage() {
  const { session } = useAuth();
  const location = useLocation();
  const alerts = useAsyncResource(
    session ? () => fetchAlerts(session.accessToken) : null,
    [session?.accessToken, "alerts"],
  );
  const backlog = useAsyncResource(
    session ? () => fetchBacklog(session.accessToken) : null,
    [session?.accessToken, "backlog"],
  );
  const drifts = useAsyncResource(
    session ? () => fetchDrifts(session.accessToken) : null,
    [session?.accessToken, "drifts"],
  );

  return (
    <div className="stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Operational posture</p>
          <h1>Alerts, backlog, drift</h1>
        </div>
        <div className="page-actions">
          <Link className="text-link" to="/channels/health">
            Channel health
          </Link>
          <Link className="text-link" to="/ops/remediation">
            Open remediation
          </Link>
        </div>
      </header>
      {location.state?.notice ? (
        <InlineBanner variant="empty">{location.state.notice}</InlineBanner>
      ) : null}
      <div className="grid-2">
        <Panel title="Outbox backlog">
          {backlog.loading ? <InlineLoading message="Loading backlog..." /> : null}
          {backlog.error ? <InlineError message={backlog.error} /> : null}
          {backlog.data ? (
            <div className="metric-grid">
              <div>
                <strong>{backlog.data.pendingCount}</strong>
                <span>Pending</span>
              </div>
              <div>
                <strong>{backlog.data.failedCount}</strong>
                <span>Failed</span>
              </div>
              <div>
                <strong>{backlog.data.retryableFailedCount}</strong>
                <span>Retryable</span>
              </div>
            </div>
          ) : null}
        </Panel>
        <Panel title="Open drifts">
          {drifts.loading ? <InlineLoading message="Loading drifts..." /> : null}
          {drifts.error ? <InlineError message={drifts.error} /> : null}
          {!drifts.loading && !drifts.error && (drifts.data ?? []).length === 0 ? (
            <EmptyState message="No open drifts are active right now." />
          ) : null}
          {(drifts.data ?? []).map((drift) => (
            <article key={drift.driftId} className="pill-row">
              <span>{drift.channel}</span>
              <strong>{drift.sku}</strong>
              <StatusBadge value={drift.status} />
            </article>
          ))}
        </Panel>
      </div>
      <Panel title="Workflow links" subtitle="Use these routes for operator remediation and channel posture checks.">
        <div className="action-row">
          <Link className="text-link" to="/channels/health">
            Open channel health
          </Link>
          <Link className="text-link" to="/ops/remediation">
            Open ops remediation
          </Link>
        </div>
      </Panel>
      <OpsCopilotPanel />
      <Panel title="Alert matrix">
        {alerts.loading ? <InlineLoading message="Loading alerts..." /> : null}
        {alerts.error ? <InlineError message={alerts.error} /> : null}
        {!alerts.loading && !alerts.error && (alerts.data ?? []).length === 0 ? (
          <EmptyState message="No alert conditions are active." />
        ) : null}
        {(alerts.data ?? []).map((alert) => (
          <article key={alert.code} className="table-row">
            <div>
              <strong>{alert.code}</strong>
              <p className="muted">{alert.message}</p>
            </div>
            <div>
              <StatusBadge value={alert.severity} />
            </div>
            <div>
              <StatusBadge value={alert.status} />
            </div>
            <div>{alert.currentValue}</div>
            <div>
              <strong>{alert.threshold}</strong>
              <p className="muted">{formatDateTime(alert.observedAt)}</p>
            </div>
          </article>
        ))}
      </Panel>
    </div>
  );
}
