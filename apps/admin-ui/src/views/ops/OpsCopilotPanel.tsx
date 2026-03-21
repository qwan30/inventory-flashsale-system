import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { InlineError, InlineLoading } from "../../components/ResourceState";
import { Panel } from "../../components/Panel";
import { StatusBadge } from "../../components/StatusBadge";
import { useAsyncResource } from "../../hooks/useAsyncResource";
import {
  fetchOpsCopilotCapabilities,
  OpsCopilotAction,
  OpsCopilotAnalysis,
  OpsCopilotCapabilities,
  runOpsCopilotAnalysis,
} from "../../lib/api";
import { useAuth } from "../../state/auth";

const FALLBACK_ACTIONS: OpsCopilotAction[] = [
  {
    label: "Open ops remediation",
    href: "/ops/remediation",
    detail: "Inspect retry queues and drift resolution workflows.",
  },
  {
    label: "Review benchmark evidence",
    href: "/benchmarks",
    detail: "Validate evidence artifacts and suite summaries.",
  },
  {
    label: "Check channel health",
    href: "/channels/health",
    detail: "Confirm marketplace posture before acting.",
  },
];

function buildLegend(caps: OpsCopilotCapabilities) {
  return (
    <div className="summary-grid">
      <div>
        <span className="muted">State</span>
        <div className="summary-inline">
          <StatusBadge value={caps.enabled ? "ENABLED" : "DISABLED"} />
          <strong>{caps.provider ?? "Ops Copilot"}</strong>
        </div>
      </div>
      <div>
        <span className="muted">Model</span>
        <strong>{caps.model ?? "-"}</strong>
      </div>
      <div>
        <span className="muted">Scopes</span>
        <p className="muted">{caps.scopes.join(", ") || "None"}</p>
      </div>
    </div>
  );
}

export function OpsCopilotPanel() {
  const { session } = useAuth();
  const capabilities = useAsyncResource<OpsCopilotCapabilities>(
    session ? () => fetchOpsCopilotCapabilities(session.accessToken) : null,
    [session?.accessToken, "ops-copilot-capabilities"],
  );
  const [analysis, setAnalysis] = useState<OpsCopilotAnalysis | null>(null);
  const [analysisLoading, setAnalysisLoading] = useState(false);
  const [analysisError, setAnalysisError] = useState<string | null>(null);

  const disabled = Boolean(capabilities.data && !capabilities.data.enabled);
  const providerMeta = analysis?.providerModel ?? capabilities.data?.model ?? "-";

  const actions = useMemo(
    () => (analysis?.recommendedActions.length ? analysis.recommendedActions : FALLBACK_ACTIONS),
    [analysis],
  );

  const handleRunAnalysis = async () => {
    if (!session || disabled) {
      return;
    }
    setAnalysisLoading(true);
    setAnalysisError(null);
    try {
      const payload = await runOpsCopilotAnalysis(session.accessToken);
      setAnalysis(payload);
    } catch (error) {
      setAnalysisError((error as Error).message);
    } finally {
      setAnalysisLoading(false);
    }
  };

  return (
    <Panel title="Ops Copilot" subtitle="Advisory insights on alerts, benchmarks, and channel posture.">
      {capabilities.loading ? (
        <InlineLoading message="Loading Ops Copilot capabilities..." />
      ) : capabilities.error ? (
        <InlineError message={capabilities.error} />
      ) : capabilities.data ? (
        <div className="stack">
          {buildLegend(capabilities.data)}
          {!capabilities.data.enabled ? (
            <InlineError
              message={capabilities.data.message ?? "Ops Copilot is disabled until a provider key is configured."}
            />
          ) : null}
          <div className="action-row">
            <button
              className="primary-button"
              disabled={disabled || analysisLoading}
              onClick={handleRunAnalysis}
            >
              {analysisLoading ? "Running Ops Copilot..." : "Run Ops Copilot"}
            </button>
            <span className="muted">Provider: {providerMeta}</span>
          </div>
          {analysisError ? <InlineError message={analysisError} /> : null}
          {analysis ? (
            <div className="stack">
              <section>
                <p className="muted">Summary</p>
                <p>{analysis.summary}</p>
              </section>
              {analysis.findings.length > 0 ? (
                <section>
                  <p className="muted">Findings</p>
                  <div className="stack">
                    {analysis.findings.map((finding) => (
                      <article key={finding.title} className="pill-row">
                        <span>
                          <StatusBadge value={finding.severity} />
                        </span>
                        <div>
                          <strong>{finding.title}</strong>
                          <p className="muted">{finding.detail}</p>
                        </div>
                      </article>
                    ))}
                  </div>
                </section>
              ) : null}
              <section>
                <p className="muted">Recommended actions</p>
                <div className="stack">
                  {actions.map((action) => (
                    <Link key={action.label} className="text-link" to={action.href}>
                      <strong>{action.label}</strong>
                      <p className="muted">{action.detail}</p>
                    </Link>
                  ))}
                </div>
              </section>
              {analysis.citations.length > 0 ? (
                <section>
                  <p className="muted">Citations</p>
                  <ul className="bullet-list">
                    {analysis.citations.map((citation) => (
                      <li key={citation}>{citation}</li>
                    ))}
                  </ul>
                </section>
              ) : null}
            </div>
          ) : null}
        </div>
      ) : null}
    </Panel>
  );
}
