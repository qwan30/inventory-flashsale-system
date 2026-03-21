export interface Credentials {
  username: string;
  password: string;
}

export interface AdminSession {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken?: string;
  refreshTokenExpiresAt?: string;
  username: string;
  displayName: string;
  role: "ADMIN" | "OPERATOR";
}

export interface CampaignItem {
  id: string;
  sku: string;
  startsAt: string;
  endsAt: string;
  quota: number;
  reservedQuota: number;
  soldQuota: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CampaignUpdateRequest {
  startsAt: string;
  endsAt: string;
  quota: number;
}

export interface AdminActivity {
  actorUsername: string;
  actorRole: "ADMIN" | "OPERATOR";
  action: string;
  resourceType: string;
  resourceId: string;
  outcome: string;
  correlationId: string;
  details: string;
  createdAt: string;
}

export interface OpsAlert {
  code: string;
  severity: string;
  status: string;
  message: string;
  currentValue: string;
  threshold: string;
  observedAt: string;
}

export interface OutboxBacklog {
  pendingCount: number;
  failedCount: number;
  retryableFailedCount: number;
}

export interface ReconciliationDrift {
  driftId: string;
  runId: string;
  channel: string;
  sku: string;
  centralInventory: InventoryDriftSnapshot;
  observedInventory: InventoryDriftSnapshot;
  status: string;
  resolutionNote?: string;
  resolvedAt?: string | null;
}

export interface InventoryDriftSnapshot {
  availableQty: number;
  reservedQty: number;
  soldQty: number;
}

export interface OutboxEventSummary {
  eventId: string;
  aggregateType: string;
  aggregateId: string;
  eventType: string;
  eventVersion: number;
  status: string;
  attempts: number;
  lastError: string | null;
  nextAttemptAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface OutboxRetryResponse {
  eventId: string;
  status: string;
  attempts: number;
  nextAttemptAt: string | null;
  lastError: string | null;
}

export interface ReconciliationRun {
  runId: string;
  triggerType: string;
  status: string;
  scannedSkuCount: number;
  scannedSnapshotCount: number;
  openDriftCount: number;
  failureMessage: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface ChannelHealthIngressReceiptSummary {
  type: string;
  externalReceiptId: string;
  outcome: string;
  processedAt: string;
}

export interface ChannelHealthReplaySummary {
  action: string;
  resourceId: string;
  outcome: string;
  createdAt: string;
  details: string;
}

export interface ChannelHealthSummary {
  channel: string;
  status: "HEALTHY" | "DEGRADED" | "UNAVAILABLE";
  connectorMode: string;
  configValid: boolean;
  syncBacklogCount: number;
  staleSnapshotCount: number;
  openDriftCount: number;
  lastReconciliationAt: string | null;
  latestIngressReceipt: ChannelHealthIngressReceiptSummary | null;
  latestReplay: ChannelHealthReplaySummary | null;
}

export interface OpsCopilotCapabilities {
  enabled: boolean;
  provider: string | null;
  model: string | null;
  scopes: string[];
  message?: string | null;
}

export interface OpsCopilotFinding {
  title: string;
  detail: string;
  severity: "INFO" | "WARN" | "CRITICAL";
}

export interface OpsCopilotAction {
  label: string;
  href: string;
  detail?: string | null;
}

export interface OpsCopilotAnalysis {
  summary: string;
  findings: OpsCopilotFinding[];
  recommendedActions: OpsCopilotAction[];
  citations: string[];
  providerModel: string;
  providerResponseId?: string | null;
}

interface OpsCopilotCapabilitiesWire {
  enabled: boolean;
  advisoryOnly: boolean;
  provider: string | null;
  model: string | null;
  allowedScopes: string[];
  scopes?: string[];
  statusMessage?: string | null;
  message?: string | null;
}

interface OpsCopilotFindingWire {
  title: string;
  detail: string;
  severity: "INFO" | "WARN" | "CRITICAL";
}

interface OpsCopilotActionWire {
  label: string;
  href: string;
  rationale?: string | null;
  detail?: string | null;
}

interface OpsCopilotCitationWire {
  sourceId: string;
  label: string;
  detail: string;
}

interface OpsCopilotAnalysisWire {
  summary: string;
  prioritizedFindings: OpsCopilotFindingWire[];
  findings?: OpsCopilotFindingWire[];
  recommendedActions: OpsCopilotActionWire[];
  citations: OpsCopilotCitationWire[] | string[];
  providerMetadata: {
    provider: string;
    model: string;
    advisoryOnly: boolean;
    requestId?: string | null;
  };
  providerModel?: string;
  providerResponseId?: string | null;
}

export interface BenchmarkEvidenceEntry {
  runId: string;
  timestamp: string;
  gitCommit: string;
  evidenceDir: string;
  suiteStatus: string;
  businessChecksPassed: boolean;
  baselineTarget?: string | null;
}

export interface BenchmarkSuiteSummary {
  suiteStatus: string;
  businessChecksPassed: boolean;
  baselineTarget: string | null;
  baselineAvailable: boolean;
  baselineNote: string | null;
}

export interface BenchmarkScenarioSummary {
  name: string;
  status: string;
  averageLatencyMs: number | null;
  p95LatencyMs: number | null;
  failedRate: number | null;
  checksRate: number | null;
  postRunChecks: string[];
}

export interface BenchmarkScenarioDelta {
  deltaAverageLatencyMs: number | null;
  deltaP95LatencyMs: number | null;
  deltaFailedRate: number | null;
  deltaChecksRate: number | null;
}

export interface BenchmarkScenarioComparison {
  scenarioName: string;
  available: boolean;
  note: string | null;
  delta: BenchmarkScenarioDelta | null;
}

export interface BenchmarkEvidenceDetail {
  entry: BenchmarkEvidenceEntry;
  manifest: Record<string, unknown>;
  report: Record<string, unknown>;
  comparison: Record<string, unknown> | null;
  summaryMarkdown: string | null;
  suiteSummary: BenchmarkSuiteSummary | null;
  scenarioSummaries: BenchmarkScenarioSummary[];
  scenarioComparisons: BenchmarkScenarioComparison[];
}

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

async function request<T>(
  path: string,
  init: RequestInit = {},
  accessToken?: string,
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Content-Type", "application/json");
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
    credentials: "include",
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || `Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export function loginRequest(credentials: Credentials) {
  return request<AdminSession>("/api/v1/admin/auth/login", {
    method: "POST",
    body: JSON.stringify(credentials),
  });
}

export function refreshRequest() {
  return request<AdminSession>("/api/v1/admin/auth/refresh", {
    method: "POST",
    body: JSON.stringify({}),
  });
}

export function logoutRequest(accessToken?: string) {
  return request<void>(
    "/api/v1/admin/auth/logout",
    {
      method: "POST",
      body: JSON.stringify({}),
    },
    accessToken,
  );
}

export function fetchCampaigns(accessToken: string) {
  return request<CampaignItem[]>("/api/v1/admin/campaigns", {}, accessToken);
}

export function fetchCampaign(accessToken: string, campaignId: string) {
  return request<CampaignItem>(`/api/v1/admin/campaigns/${campaignId}`, {}, accessToken);
}

export function updateCampaign(
  accessToken: string,
  campaignId: string,
  payload: CampaignUpdateRequest,
) {
  return request<CampaignItem>(
    `/api/v1/admin/campaigns/${campaignId}`,
    {
      method: "PUT",
      body: JSON.stringify(payload),
    },
    accessToken,
  );
}

export function activateCampaign(accessToken: string, campaignId: string) {
  return request<CampaignItem>(
    `/api/v1/admin/campaigns/${campaignId}/activate`,
    { method: "POST" },
    accessToken,
  );
}

export function endCampaign(accessToken: string, campaignId: string) {
  return request<CampaignItem>(
    `/api/v1/admin/campaigns/${campaignId}/end`,
    { method: "POST" },
    accessToken,
  );
}

export function fetchCampaignAudits(accessToken: string, campaignId: string) {
  return request<AdminActivity[]>(
    `/api/v1/admin/campaigns/${campaignId}/audits`,
    {},
    accessToken,
  );
}

export function fetchAlerts(accessToken: string) {
  return request<OpsAlert[]>("/api/v1/admin/ops/alerts", {}, accessToken);
}

export function fetchBacklog(accessToken: string) {
  return request<OutboxBacklog>("/api/v1/admin/ops/outbox/backlog", {}, accessToken);
}

export function fetchOutboxEvents(accessToken: string) {
  return request<OutboxEventSummary[]>(
    "/api/v1/admin/ops/outbox/events",
    {},
    accessToken,
  );
}

export function retryOutboxEvent(accessToken: string, eventId: string) {
  return request<OutboxRetryResponse>(
    `/api/v1/admin/ops/outbox/${eventId}/retry`,
    { method: "POST" },
    accessToken,
  );
}

export function fetchDrifts(accessToken: string) {
  return request<ReconciliationDrift[]>(
    "/api/v1/admin/ops/reconciliation/drifts",
    {},
    accessToken,
  );
}

export function resolveDrift(
  accessToken: string,
  driftId: string,
  resolutionNote: string,
) {
  return request<ReconciliationDrift>(
    `/api/v1/admin/ops/reconciliation/${driftId}/resolve`,
    {
      method: "POST",
      body: JSON.stringify({ resolutionNote }),
    },
    accessToken,
  );
}

export function fetchReconciliationRuns(accessToken: string) {
  return request<ReconciliationRun[]>(
    "/api/v1/admin/ops/reconciliation/runs",
    {},
    accessToken,
  );
}

export function fetchChannelHealth(accessToken: string) {
  return request<ChannelHealthSummary[]>(
    "/api/v1/admin/channels/health",
    {},
    accessToken,
  );
}

export function runReconciliation(accessToken: string) {
  return request<ReconciliationRun>(
    "/api/v1/admin/ops/reconciliation/runs",
    { method: "POST" },
    accessToken,
  );
}

export function fetchBenchmarkEvidence(accessToken: string) {
  return request<BenchmarkEvidenceEntry[]>(
    "/api/v1/admin/ops/benchmarks/evidence",
    {},
    accessToken,
  );
}

export function fetchBenchmarkDetail(accessToken: string, runId: string) {
  return request<BenchmarkEvidenceDetail>(
    `/api/v1/admin/ops/benchmarks/evidence/${runId}`,
    {},
    accessToken,
  );
}

export function fetchLatestBenchmarkEvidence(accessToken: string) {
  return request<BenchmarkEvidenceDetail>(
    "/api/v1/admin/ops/benchmarks/evidence/latest",
    {},
    accessToken,
  );
}

export function fetchOpsCopilotCapabilities(accessToken: string) {
  return request<OpsCopilotCapabilitiesWire>(
    "/api/v1/admin/ops/copilot/capabilities",
    {},
    accessToken,
  ).then((payload) => ({
    enabled: payload.enabled,
    provider: payload.provider,
    model: payload.model,
    scopes: payload.allowedScopes ?? payload.scopes ?? [],
    message: payload.statusMessage ?? payload.message,
  }));
}

export function runOpsCopilotAnalysis(accessToken: string, scope?: string) {
  return request<OpsCopilotAnalysisWire>(
    "/api/v1/admin/ops/copilot/analyze",
    {
      method: "POST",
      body: JSON.stringify({ scope: scope ?? "OPS_OVERVIEW" }),
    },
    accessToken,
  ).then((payload) => ({
    summary: payload.summary,
    findings: payload.prioritizedFindings ?? payload.findings ?? [],
    recommendedActions: (payload.recommendedActions ?? []).map((action) => ({
      label: action.label,
      href: action.href,
      detail: action.rationale ?? action.detail ?? null,
    })),
    citations: (payload.citations ?? []).map((citation) =>
      typeof citation === "string" ? citation : `${citation.label}: ${citation.detail}`,
    ),
    providerModel: payload.providerMetadata?.model ?? payload.providerModel ?? "unknown",
    providerResponseId: payload.providerMetadata?.requestId ?? payload.providerResponseId ?? null,
  }));
}
