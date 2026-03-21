import { Link } from "react-router-dom";
import { Panel } from "../../components/Panel";
import { EmptyState, InlineError, InlineLoading } from "../../components/ResourceState";
import { StatusBadge } from "../../components/StatusBadge";
import { useAuth } from "../../state/auth";
import { useAsyncResource } from "../../hooks/useAsyncResource";
import { fetchBenchmarkEvidence } from "../../lib/api";
import { formatDateTime } from "../../lib/format";

export function BenchmarksPage() {
  const { session } = useAuth();
  const evidence = useAsyncResource(
    session ? () => fetchBenchmarkEvidence(session.accessToken) : null,
    [session?.accessToken, "benchmark-index"],
  );

  return (
    <div className="stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Evidence program</p>
          <h1>Benchmark reporting</h1>
        </div>
      </header>
      <Panel title="Promoted runs" subtitle="Only promoted evidence is surfaced to the UI.">
        {evidence.loading ? <InlineLoading message="Loading promoted benchmark runs..." /> : null}
        {evidence.error ? <InlineError message={evidence.error} /> : null}
        {!evidence.loading && !evidence.error && (evidence.data ?? []).length === 0 ? (
          <EmptyState message="No promoted benchmark runs are available." />
        ) : null}
        {(evidence.data ?? []).map((entry) => (
          <Link
            key={entry.runId}
            className="run-card run-card-link"
            to={`/benchmarks/${entry.runId}`}
          >
            <strong>{entry.runId}</strong>
            <div className="summary-inline">
              <StatusBadge value={entry.suiteStatus} />
              {entry.baselineTarget ? <span className="muted">vs {entry.baselineTarget}</span> : null}
            </div>
            <span>{entry.gitCommit}</span>
            <span className="muted">{formatDateTime(entry.timestamp)}</span>
          </Link>
        ))}
      </Panel>
    </div>
  );
}
