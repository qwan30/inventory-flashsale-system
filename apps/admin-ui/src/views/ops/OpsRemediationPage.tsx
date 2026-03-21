import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Panel } from "../../components/Panel";
import {
  EmptyState,
  InlineError,
  InlineLoading,
  InlineSuccess,
} from "../../components/ResourceState";
import { StatusBadge } from "../../components/StatusBadge";
import { useAsyncAction } from "../../hooks/useAsyncAction";
import { useAsyncResource } from "../../hooks/useAsyncResource";
import {
  fetchDrifts,
  fetchOutboxEvents,
  fetchReconciliationRuns,
  resolveDrift,
  retryOutboxEvent,
  runReconciliation,
} from "../../lib/api";
import { formatDateTime, formatNumber } from "../../lib/format";
import { useAuth } from "../../state/auth";

export function OpsRemediationPage() {
  const { session } = useAuth();
  const [activeTab, setActiveTab] = useState<"outbox" | "runs" | "drifts">("outbox");
  const [resolutionNotes, setResolutionNotes] = useState<Record<string, string>>({});
  const outbox = useAsyncResource(
    session ? () => fetchOutboxEvents(session.accessToken) : null,
    [session?.accessToken, "outbox-events"],
  );
  const runs = useAsyncResource(
    session ? () => fetchReconciliationRuns(session.accessToken) : null,
    [session?.accessToken, "reconciliation-runs"],
  );
  const drifts = useAsyncResource(
    session ? () => fetchDrifts(session.accessToken) : null,
    [session?.accessToken, "reconciliation-drifts"],
  );
  const retryAction = useAsyncAction();
  const runAction = useAsyncAction();
  const resolveAction = useAsyncAction();

  const latestRun = useMemo(() => {
    return (runs.data ?? [])[0] ?? null;
  }, [runs.data]);

  return (
    <div className="stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Operational posture</p>
          <h1>Ops remediation</h1>
          <p className="muted">
            Drive retry, reconciliation, and drift resolution without leaving the admin shell.
          </p>
        </div>
        <div className="page-actions">
          <button
            className="primary-button"
            onClick={() =>
              void runAction
                .run(
                  () => runReconciliation(session!.accessToken),
                  "Reconciliation run triggered.",
                )
                .then(() => {
                  runs.reload();
                  drifts.reload();
                })
            }
            disabled={runAction.pending}
          >
            {runAction.pending ? "Running reconciliation..." : "Run reconciliation"}
          </button>
          <Link className="text-link" to="/ops">
            Back to ops
          </Link>
        </div>
      </header>
      {runAction.error ? <InlineError message={runAction.error} /> : null}
      {runAction.success ? <InlineSuccess message={runAction.success} /> : null}
      <Panel
        title="Remediation workspace"
        subtitle={
          latestRun
            ? `Latest run ${latestRun.runId} • ${formatDateTime(latestRun.createdAt)}`
            : "Open the tab that matches the operational issue you need to remediate."
        }
      >
        <div className="tab-strip" role="tablist" aria-label="Ops remediation tabs">
          <button
            className={`tab-button ${activeTab === "outbox" ? "tab-button-active" : ""}`}
            onClick={() => setActiveTab("outbox")}
            role="tab"
            aria-selected={activeTab === "outbox"}
          >
            Outbox failures
          </button>
          <button
            className={`tab-button ${activeTab === "runs" ? "tab-button-active" : ""}`}
            onClick={() => setActiveTab("runs")}
            role="tab"
            aria-selected={activeTab === "runs"}
          >
            Reconciliation runs
          </button>
          <button
            className={`tab-button ${activeTab === "drifts" ? "tab-button-active" : ""}`}
            onClick={() => setActiveTab("drifts")}
            role="tab"
            aria-selected={activeTab === "drifts"}
          >
            Drift detail
          </button>
        </div>

        {activeTab === "outbox" ? (
          <div className="stack">
            {retryAction.error ? <InlineError message={retryAction.error} /> : null}
            {retryAction.success ? <InlineSuccess message={retryAction.success} /> : null}
            {outbox.loading ? <InlineLoading message="Loading failed outbox events..." /> : null}
            {outbox.error ? <InlineError message={outbox.error} /> : null}
            {!outbox.loading && !outbox.error && (outbox.data ?? []).length === 0 ? (
              <EmptyState message="No failed outbox events need remediation." />
            ) : null}
            {(outbox.data ?? []).length > 0 ? (
              <div className="table-grid">
                <div className="table-row table-row-head">
                  <div>Event</div>
                  <div>Status</div>
                  <div>Attempts</div>
                  <div>Last error</div>
                  <div>Updated</div>
                  <div>Action</div>
                </div>
                {(outbox.data ?? []).map((event) => (
                  <div key={event.eventId} className="table-row table-row-remediation">
                    <div>
                      <strong>{event.eventId}</strong>
                      <p className="muted">{event.eventType}</p>
                    </div>
                    <div>
                      <StatusBadge value={event.status} />
                    </div>
                    <div>{event.attempts}</div>
                    <div>{event.lastError ?? "Not available"}</div>
                    <div>{formatDateTime(event.updatedAt)}</div>
                    <div>
                      <button
                        className="ghost-button ghost-button-dark"
                        onClick={() =>
                          void retryAction
                            .run(
                              () => retryOutboxEvent(session!.accessToken, event.eventId),
                              `Retry queued for ${event.eventId}.`,
                            )
                            .then(() => outbox.reload())
                        }
                        disabled={retryAction.pending}
                      >
                        Retry
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            ) : null}
          </div>
        ) : null}

        {activeTab === "runs" ? (
          <div className="stack">
            {runs.loading ? <InlineLoading message="Loading reconciliation runs..." /> : null}
            {runs.error ? <InlineError message={runs.error} /> : null}
            {!runs.loading && !runs.error && (runs.data ?? []).length === 0 ? (
              <EmptyState message="No reconciliation runs have been recorded yet." />
            ) : null}
            {(runs.data ?? []).length > 0 ? (
              <div className="table-grid">
                <div className="table-row table-row-head">
                  <div>Run</div>
                  <div>Trigger</div>
                  <div>Status</div>
                  <div>Scanned</div>
                  <div>Open drift</div>
                  <div>Completed</div>
                </div>
                {(runs.data ?? []).map((run) => (
                  <div key={run.runId} className="table-row table-row-remediation">
                    <div>
                      <strong>{run.runId}</strong>
                      <p className="muted">{formatDateTime(run.createdAt)}</p>
                    </div>
                    <div>{run.triggerType}</div>
                    <div>
                      <StatusBadge value={run.status} />
                    </div>
                    <div>
                      {formatNumber(run.scannedSkuCount)} SKUs
                      <p className="muted">{formatNumber(run.scannedSnapshotCount)} snapshots</p>
                    </div>
                    <div>{formatNumber(run.openDriftCount)}</div>
                    <div>{formatDateTime(run.completedAt)}</div>
                  </div>
                ))}
              </div>
            ) : null}
          </div>
        ) : null}

        {activeTab === "drifts" ? (
          <div className="stack">
            {resolveAction.error ? <InlineError message={resolveAction.error} /> : null}
            {resolveAction.success ? <InlineSuccess message={resolveAction.success} /> : null}
            {drifts.loading ? <InlineLoading message="Loading reconciliation drifts..." /> : null}
            {drifts.error ? <InlineError message={drifts.error} /> : null}
            {!drifts.loading && !drifts.error && (drifts.data ?? []).length === 0 ? (
              <EmptyState message="No open drifts are waiting for resolution." />
            ) : null}
            {(drifts.data ?? []).map((drift) => (
              <div key={drift.driftId} className="remediation-card">
                <div className="summary-grid">
                  <div className="summary-item">
                    <span className="muted">Channel</span>
                    <strong>{drift.channel}</strong>
                  </div>
                  <div className="summary-item">
                    <span className="muted">SKU</span>
                    <strong>{drift.sku}</strong>
                  </div>
                  <div className="summary-item">
                    <span className="muted">Status</span>
                    <div className="summary-inline">
                      <StatusBadge value={drift.status} />
                    </div>
                  </div>
                  <div className="summary-item">
                    <span className="muted">Run</span>
                    <strong>{drift.runId}</strong>
                  </div>
                </div>
                <div className="grid-2">
                  <Panel title="Central snapshot">
                    <SnapshotBlock
                      availableQty={drift.centralInventory.availableQty}
                      reservedQty={drift.centralInventory.reservedQty}
                      soldQty={drift.centralInventory.soldQty}
                    />
                  </Panel>
                  <Panel title="Observed snapshot">
                    <SnapshotBlock
                      availableQty={drift.observedInventory.availableQty}
                      reservedQty={drift.observedInventory.reservedQty}
                      soldQty={drift.observedInventory.soldQty}
                    />
                  </Panel>
                </div>
                <div className="form-grid">
                  <label className="form-field">
                    <span>Resolution note</span>
                    <textarea
                      value={resolutionNotes[drift.driftId] ?? drift.resolutionNote ?? ""}
                      onChange={(event) =>
                        setResolutionNotes((current) => ({
                          ...current,
                          [drift.driftId]: event.target.value,
                        }))
                      }
                      rows={3}
                    />
                  </label>
                  <div className="action-row">
                    <button
                      className="primary-button"
                      onClick={() =>
                        void resolveAction
                          .run(
                            () =>
                              resolveDrift(
                                session!.accessToken,
                                drift.driftId,
                                resolutionNotes[drift.driftId] ?? drift.resolutionNote ?? "",
                              ),
                            `Drift ${drift.driftId} resolved.`,
                          )
                          .then(() => {
                            drifts.reload();
                            runs.reload();
                          })
                      }
                      disabled={
                        resolveAction.pending ||
                        !(resolutionNotes[drift.driftId] ?? drift.resolutionNote ?? "").trim()
                      }
                    >
                      Resolve drift
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : null}
      </Panel>
    </div>
  );
}

function SnapshotBlock({
  availableQty,
  reservedQty,
  soldQty,
}: {
  availableQty: number;
  reservedQty: number;
  soldQty: number;
}) {
  return (
    <div className="summary-grid">
      <div className="summary-item">
        <span className="muted">Available</span>
        <strong>{formatNumber(availableQty)}</strong>
      </div>
      <div className="summary-item">
        <span className="muted">Reserved</span>
        <strong>{formatNumber(reservedQty)}</strong>
      </div>
      <div className="summary-item">
        <span className="muted">Sold</span>
        <strong>{formatNumber(soldQty)}</strong>
      </div>
    </div>
  );
}
