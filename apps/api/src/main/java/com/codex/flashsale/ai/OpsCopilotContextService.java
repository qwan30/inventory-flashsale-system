package com.codex.flashsale.ai;

import com.codex.flashsale.admin.AdminActivityAudit;
import com.codex.flashsale.admin.AdminActivityAuditService;
import com.codex.flashsale.admin.AdminActivityResourceType;
import com.codex.flashsale.api.AdminActivityResponse;
import com.codex.flashsale.api.AdminCampaignResponse;
import com.codex.flashsale.api.OpsCopilotAnalyzeRequest;
import com.codex.flashsale.application.AdminCampaignApplicationService;
import com.codex.flashsale.application.ChannelHealthSummary;
import com.codex.flashsale.application.OpsApplicationService;
import com.codex.flashsale.benchmark.BenchmarkEvidenceService;
import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.exception.BadRequestException;
import com.codex.flashsale.common.exception.NotFoundException;
import com.codex.flashsale.outbox.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpsCopilotContextService {

    private static final int DETAIL_LIMIT = 5;

    private final OpsApplicationService opsApplicationService;
    private final BenchmarkEvidenceService benchmarkEvidenceService;
    private final AdminCampaignApplicationService adminCampaignApplicationService;
    private final AdminActivityAuditService adminActivityAuditService;
    private final ObjectMapper objectMapper;

    public OpsCopilotContextService(
            OpsApplicationService opsApplicationService,
            BenchmarkEvidenceService benchmarkEvidenceService,
            AdminCampaignApplicationService adminCampaignApplicationService,
            AdminActivityAuditService adminActivityAuditService,
            ObjectMapper objectMapper
    ) {
        this.opsApplicationService = opsApplicationService;
        this.benchmarkEvidenceService = benchmarkEvidenceService;
        this.adminCampaignApplicationService = adminCampaignApplicationService;
        this.adminActivityAuditService = adminActivityAuditService;
        this.objectMapper = objectMapper;
    }

    public OpsCopilotContext buildContext(OpsCopilotScope scope, OpsCopilotAnalyzeRequest request) {
        return switch (scope) {
            case OPS_OVERVIEW -> buildOpsOverviewContext(request);
            case BENCHMARK_RUN -> buildBenchmarkContext(request);
            case CHANNEL_HEALTH -> buildChannelHealthContext(request);
            case CAMPAIGN_AUDIT -> buildCampaignAuditContext(request);
        };
    }

    private OpsCopilotContext buildOpsOverviewContext(OpsCopilotAnalyzeRequest request) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("alerts", opsApplicationService.getAlerts());
        facts.put("outboxBacklog", opsApplicationService.getOutboxBacklog());
        facts.put("failedOutboxEvents", opsApplicationService.listOutboxEvents(OutboxStatus.FAILED, DETAIL_LIMIT));
        facts.put("openReconciliationDrifts", opsApplicationService.listOpenReconciliationDrifts().stream().limit(DETAIL_LIMIT).toList());
        facts.put("recentReconciliationRuns", opsApplicationService.listReconciliationRuns(DETAIL_LIMIT));
        facts.put("channelHealth", opsApplicationService.listChannelHealthSummaries());

        return new OpsCopilotContext(
                OpsCopilotScope.OPS_OVERVIEW,
                "ops-overview",
                request.focusQuestion(),
                objectMapper.valueToTree(facts),
                List.of(
                        new OpsCopilotSource("alerts-current", "Current ops alerts", "Current alert surface from GET /api/v1/admin/ops/alerts"),
                        new OpsCopilotSource("outbox-backlog", "Outbox backlog", "Current backlog counts from GET /api/v1/admin/ops/outbox/backlog"),
                        new OpsCopilotSource("outbox-failed-events", "Failed outbox events", "Newest failed outbox events from GET /api/v1/admin/ops/outbox/events"),
                        new OpsCopilotSource("reconciliation-open-drifts", "Open reconciliation drifts", "Current unresolved drift records"),
                        new OpsCopilotSource("reconciliation-runs", "Recent reconciliation runs", "Recent operator and scheduled reconciliation runs"),
                        new OpsCopilotSource("channel-health-summaries", "Channel health summaries", "Marketplace posture summary cards")
                ),
                List.of("/ops", "/ops/remediation", "/channels/health", "/benchmarks")
        );
    }

    private OpsCopilotContext buildBenchmarkContext(OpsCopilotAnalyzeRequest request) {
        String runId = requireValue(request.benchmarkRunId(), "OPS_COPILOT_BENCHMARK_RUN_REQUIRED", "Benchmark run id is required");
        var detail = benchmarkEvidenceService.getEvidence(runId);

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("entry", detail.entry());
        facts.put("suiteSummary", detail.suiteSummary());
        facts.put("scenarioSummaries", detail.scenarioSummaries());
        facts.put("scenarioComparisons", detail.scenarioComparisons());
        facts.put("summaryMarkdown", detail.summaryMarkdown());

        return new OpsCopilotContext(
                OpsCopilotScope.BENCHMARK_RUN,
                runId,
                request.focusQuestion(),
                objectMapper.valueToTree(facts),
                List.of(
                        new OpsCopilotSource(
                                "benchmark-evidence-%s".formatted(runId),
                                "Benchmark evidence %s".formatted(runId),
                                "Typed benchmark evidence summary for run %s".formatted(runId)
                        )
                ),
                List.of("/benchmarks", "/benchmarks/%s".formatted(runId), "/ops")
        );
    }

    private OpsCopilotContext buildChannelHealthContext(OpsCopilotAnalyzeRequest request) {
        List<ChannelHealthSummary> summaries = opsApplicationService.listChannelHealthSummaries();
        String channelValue = request.channel();
        ChannelHealthSummary summary = null;
        if (channelValue != null && !channelValue.isBlank()) {
            SalesChannel channel = parseChannel(channelValue);
            summary = summaries.stream()
                    .filter(candidate -> candidate.channel() == channel)
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("CHANNEL_HEALTH_NOT_FOUND", "Channel health not found for " + channelValue));
        }

        ChannelHealthSummary selectedSummary = summary;
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("selectedChannel", selectedSummary);
        facts.put("channelHealth", selectedSummary == null ? summaries : List.of(selectedSummary));
        facts.put("recentReconciliationRuns", opsApplicationService.listReconciliationRuns(DETAIL_LIMIT));
        facts.put("openReconciliationDrifts", opsApplicationService.listOpenReconciliationDrifts().stream()
                .filter(drift -> selectedSummary == null || drift.channel() == selectedSummary.channel())
                .limit(DETAIL_LIMIT)
                .toList());

        String subjectId = selectedSummary == null ? "all-marketplaces" : selectedSummary.channel().name();
        return new OpsCopilotContext(
                OpsCopilotScope.CHANNEL_HEALTH,
                subjectId,
                request.focusQuestion(),
                objectMapper.valueToTree(facts),
                List.of(
                        new OpsCopilotSource("channel-health-%s".formatted(subjectId), "Channel health", "Marketplace posture data for %s".formatted(subjectId)),
                        new OpsCopilotSource("channel-reconciliation-runs", "Recent reconciliation runs", "Recent reconciliation timing and outcomes"),
                        new OpsCopilotSource("channel-open-drifts", "Channel drifts", "Current open reconciliation drifts for this view")
                ),
                List.of("/channels/health", "/ops", "/ops/remediation")
        );
    }

    private OpsCopilotContext buildCampaignAuditContext(OpsCopilotAnalyzeRequest request) {
        String campaignId = requireValue(request.campaignId(), "OPS_COPILOT_CAMPAIGN_REQUIRED", "Campaign id is required");
        AdminCampaignResponse campaign = adminCampaignApplicationService.getCampaign(campaignId);
        List<AdminActivityResponse> audits = adminActivityAuditService.findResourceActivity(AdminActivityResourceType.CAMPAIGN, campaignId)
                .stream()
                .limit(DETAIL_LIMIT)
                .map(this::toAdminActivityResponse)
                .toList();

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("campaign", campaign);
        facts.put("recentAudits", audits);

        return new OpsCopilotContext(
                OpsCopilotScope.CAMPAIGN_AUDIT,
                campaignId,
                request.focusQuestion(),
                objectMapper.valueToTree(facts),
                List.of(
                        new OpsCopilotSource("campaign-%s".formatted(campaignId), "Campaign detail", "Campaign lifecycle detail for %s".formatted(campaignId)),
                        new OpsCopilotSource("campaign-audits-%s".formatted(campaignId), "Campaign audits", "Recent immutable campaign audit entries")
                ),
                List.of("/campaigns/%s".formatted(campaignId), "/campaigns/%s/audits".formatted(campaignId))
        );
    }

    private String requireValue(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(code, message);
        }
        return value;
    }

    private SalesChannel parseChannel(String rawValue) {
        try {
            return SalesChannel.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("OPS_COPILOT_CHANNEL_UNSUPPORTED", "Unsupported channel: " + rawValue);
        }
    }

    private AdminActivityResponse toAdminActivityResponse(AdminActivityAudit audit) {
        return new AdminActivityResponse(
                audit.getActorUsername(),
                audit.getActorRole(),
                audit.getAction(),
                audit.getResourceType(),
                audit.getResourceId(),
                audit.getOutcome(),
                audit.getCorrelationId(),
                audit.getDetails(),
                audit.getCreatedAt()
        );
    }
}
