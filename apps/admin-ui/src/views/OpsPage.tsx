import { Panel } from "../components/Panel";
import { useAuth } from "../state/auth";
import { useAsyncResource } from "../hooks/useAsyncResource";
import { fetchAlerts, fetchBacklog, fetchDrifts } from "../lib/api";

export function OpsPage() {
  const { session } = useAuth();
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
      </header>
      <div className="grid-2">
        <Panel title="Outbox backlog">
          {backlog.loading ? <p>Loading backlog...</p> : null}
          {backlog.data ? (
            <div className="metric-grid">
              <div><strong>{backlog.data.pendingCount}</strong><span>Pending</span></div>
              <div><strong>{backlog.data.failedCount}</strong><span>Failed</span></div>
              <div><strong>{backlog.data.retryableFailedCount}</strong><span>Retryable</span></div>
            </div>
          ) : null}
        </Panel>
        <Panel title="Open drifts">
          {drifts.loading ? <p>Loading drifts...</p> : null}
          {(drifts.data ?? []).map((drift) => (
            <article key={drift.driftId} className="pill-row">
              <span>{drift.channel}</span>
              <strong>{drift.sku}</strong>
              <span>{drift.status}</span>
            </article>
          ))}
        </Panel>
      </div>
      <Panel title="Alert matrix">
        {alerts.loading ? <p>Loading alerts...</p> : null}
        {(alerts.data ?? []).map((alert) => (
          <article key={alert.code} className="table-row">
            <div>
              <strong>{alert.code}</strong>
              <p className="muted">{alert.message}</p>
            </div>
            <div>{alert.severity}</div>
            <div>{alert.status}</div>
            <div>{alert.currentValue}</div>
            <div>{alert.threshold}</div>
          </article>
        ))}
      </Panel>
    </div>
  );
}
