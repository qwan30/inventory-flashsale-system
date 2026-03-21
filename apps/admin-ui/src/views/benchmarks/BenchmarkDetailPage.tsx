import { useMemo } from "react";
import { Link, useParams } from "react-router-dom";
import { Panel } from "../../components/Panel";
import { EmptyState, InlineError, InlineLoading } from "../../components/ResourceState";
import { StatusBadge } from "../../components/StatusBadge";
import { useAsyncResource } from "../../hooks/useAsyncResource";
import { fetchBenchmarkDetail } from "../../lib/api";
import { formatDateTime, formatDelta, formatNumber, formatPercent } from "../../lib/format";
import { useAuth } from "../../state/auth";

export function BenchmarkDetailPage() {
  const { runId } = useParams();
  const { session } = useAuth();
  const detail = useAsyncResource(
    session && runId ? () => fetchBenchmarkDetail(session.accessToken, runId) : null,
    [session?.accessToken, runId],
  );
  const overallComparisonNote = useMemo(() => {
    const comparisons = detail.data?.scenarioComparisons ?? [];
    const availableComparisons = comparisons.filter((comparison) => comparison.available);

    if (availableComparisons.length === 0) {
      return "No baseline comparison is available for this run.";
    }

    const regressions = availableComparisons.filter(
      (comparison) =>
        (comparison.delta?.deltaAverageLatencyMs ?? 0) > 0 ||
        (comparison.delta?.deltaP95LatencyMs ?? 0) > 0 ||
        (comparison.delta?.deltaFailedRate ?? 0) > 0,
    ).length;

    if (regressions === 0) {
      return "This run is equal to or better than its baseline on the available scenario comparisons.";
    }

    return `${regressions} scenario comparison(s) show regression against baseline.`;
  }, [detail.data?.scenarioComparisons]);

  return (
    <div className="stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Evidence program</p>
          <h1>Benchmark detail</h1>
          <p className="muted">Operator-readable benchmark evidence and baseline comparison.</p>
        </div>
        <div className="page-actions">
          <Link className="text-link" to="/benchmarks">
            Back to benchmarks
          </Link>
        </div>
      </header>
      {detail.loading ? <InlineLoading message="Loading benchmark detail..." /> : null}
      {detail.error ? <InlineError message={detail.error} /> : null}
      {!detail.loading && !detail.error && !detail.data ? (
        <EmptyState message="Benchmark evidence could not be loaded." />
      ) : null}
      {detail.data ? (
        <>
          <Panel
            title={detail.data.entry.runId}
            subtitle={`Commit ${detail.data.entry.gitCommit} • ${formatDateTime(detail.data.entry.timestamp)}`}
          >
            <div className="summary-grid">
              <div className="summary-item">
                <span className="muted">Suite status</span>
                <div className="summary-inline">
                  <StatusBadge
                    value={detail.data.suiteSummary?.suiteStatus ?? detail.data.entry.suiteStatus}
                  />
                </div>
              </div>
              <div className="summary-item">
                <span className="muted">Business checks</span>
                <strong>{detail.data.entry.businessChecksPassed ? "Passed" : "Failed"}</strong>
              </div>
              <div className="summary-item">
                <span className="muted">Baseline target</span>
                <strong>{detail.data.entry.baselineTarget ?? "Not available"}</strong>
              </div>
              <div className="summary-item">
                <span className="muted">Comparison note</span>
                <strong>{detail.data.suiteSummary?.baselineNote ?? overallComparisonNote}</strong>
              </div>
            </div>
          </Panel>

          <Panel title="Scenario summary" subtitle="Typed scenario metrics surfaced from the benchmark evidence API.">
            {(detail.data.scenarioSummaries ?? []).length === 0 ? (
              <EmptyState message="No typed scenario summary was available for this run." />
            ) : (
              <div className="table-grid">
                <div className="table-row table-row-head">
                  <div>Scenario</div>
                  <div>Status</div>
                  <div>Average</div>
                  <div>P95</div>
                  <div>Failed rate</div>
                  <div>Checks rate</div>
                </div>
                {detail.data.scenarioSummaries.map((scenario) => (
                  <div key={scenario.name} className="table-row table-row-remediation">
                    <div>
                      <strong>{scenario.name}</strong>
                      <p className="muted">
                        {scenario.postRunChecks.join(", ") || "No post-run checks"}
                      </p>
                    </div>
                    <div>
                      <StatusBadge value={scenario.status} />
                    </div>
                    <div>{formatNumber(scenario.averageLatencyMs)}</div>
                    <div>{formatNumber(scenario.p95LatencyMs)}</div>
                    <div>{formatPercent(scenario.failedRate)}</div>
                    <div>{formatPercent(scenario.checksRate)}</div>
                  </div>
                ))}
              </div>
            )}
          </Panel>

          <Panel title="Baseline comparison" subtitle={overallComparisonNote}>
            {(detail.data.scenarioComparisons ?? []).length === 0 ? (
              <EmptyState message="No baseline comparison data is available for this run." />
            ) : (
              <div className="comparison-grid">
                {detail.data.scenarioComparisons.map((comparison) => (
                  <article
                    key={comparison.scenarioName}
                    className={`comparison-card ${comparison.available ? "" : "comparison-card-muted"}`}
                  >
                    <div className="summary-inline">
                      <strong>{comparison.scenarioName}</strong>
                      <StatusBadge value={comparison.available ? "AVAILABLE" : "MISSING"} />
                    </div>
                    <p className="muted">{comparison.note ?? "No comparison note available."}</p>
                    {comparison.available && comparison.delta ? (
                      <div className="comparison-deltas">
                        <div className={deltaClassName(comparison.delta.deltaAverageLatencyMs)}>
                          Avg {formatDelta(comparison.delta.deltaAverageLatencyMs, " ms")}
                        </div>
                        <div className={deltaClassName(comparison.delta.deltaP95LatencyMs)}>
                          P95 {formatDelta(comparison.delta.deltaP95LatencyMs, " ms")}
                        </div>
                        <div className={deltaClassName(comparison.delta.deltaFailedRate)}>
                          Failed {formatDelta(comparison.delta.deltaFailedRate)}
                        </div>
                        <div className={inverseDeltaClassName(comparison.delta.deltaChecksRate)}>
                          Checks {formatDelta(comparison.delta.deltaChecksRate)}
                        </div>
                      </div>
                    ) : null}
                  </article>
                ))}
              </div>
            )}
          </Panel>

          <div className="grid-2">
            <Panel title="Summary markdown">
              <pre className="code-panel">
                {detail.data.summaryMarkdown ?? "No summary markdown available."}
              </pre>
            </Panel>
            <Panel title="Raw evidence inspectors">
              <details className="stack">
                <summary>Manifest</summary>
                <pre className="code-panel">{JSON.stringify(detail.data.manifest, null, 2)}</pre>
              </details>
              <details className="stack">
                <summary>Report</summary>
                <pre className="code-panel">{JSON.stringify(detail.data.report, null, 2)}</pre>
              </details>
            </Panel>
          </div>
        </>
      ) : null}
    </div>
  );
}

function deltaClassName(value: number | null) {
  if (value == null) {
    return "delta-pill";
  }

  if (value > 0) {
    return "delta-pill delta-pill-negative";
  }

  if (value < 0) {
    return "delta-pill delta-pill-positive";
  }

  return "delta-pill";
}

function inverseDeltaClassName(value: number | null) {
  if (value == null) {
    return "delta-pill";
  }

  if (value > 0) {
    return "delta-pill delta-pill-positive";
  }

  if (value < 0) {
    return "delta-pill delta-pill-negative";
  }

  return "delta-pill";
}
