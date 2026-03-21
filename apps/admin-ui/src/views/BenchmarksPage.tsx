import { useState } from "react";
import { Panel } from "../components/Panel";
import { useAuth } from "../state/auth";
import { useAsyncResource } from "../hooks/useAsyncResource";
import {
  fetchBenchmarkDetail,
  fetchBenchmarkEvidence,
} from "../lib/api";

export function BenchmarksPage() {
  const { session } = useAuth();
  const evidence = useAsyncResource(
    session ? () => fetchBenchmarkEvidence(session.accessToken) : null,
    [session?.accessToken, "benchmark-index"],
  );
  const [selectedRunId, setSelectedRunId] = useState<string | null>(null);
  const detail = useAsyncResource(
    session && selectedRunId
      ? () => fetchBenchmarkDetail(session.accessToken, selectedRunId)
      : null,
    [session?.accessToken, selectedRunId],
  );

  return (
    <div className="stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Evidence program</p>
          <h1>Benchmark reporting</h1>
        </div>
      </header>
      <div className="grid-2">
        <Panel title="Promoted runs" subtitle="Only promoted evidence is surfaced to the UI.">
          {(evidence.data ?? []).map((entry) => (
            <button
              key={entry.runId}
              className="run-card"
              onClick={() => setSelectedRunId(entry.runId)}
            >
              <strong>{entry.runId}</strong>
              <span>{entry.suiteStatus}</span>
              <span>{entry.gitCommit}</span>
            </button>
          ))}
        </Panel>
        <Panel title="Run detail">
          {!selectedRunId ? <p>Select a run to inspect manifest/report detail.</p> : null}
          {detail.loading ? <p>Loading run detail...</p> : null}
          {detail.data ? (
            <div className="stack">
              <div className="metric-grid">
                <div><strong>{detail.data.entry.suiteStatus}</strong><span>Suite status</span></div>
                <div><strong>{String(detail.data.entry.businessChecksPassed)}</strong><span>Business checks</span></div>
                <div><strong>{detail.data.entry.gitCommit}</strong><span>Commit</span></div>
              </div>
              <pre className="code-panel">
                {JSON.stringify(detail.data.report, null, 2)}
              </pre>
            </div>
          ) : null}
        </Panel>
      </div>
    </div>
  );
}
