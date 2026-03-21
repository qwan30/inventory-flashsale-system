package com.codex.flashsale.controller;

import com.codex.flashsale.admin.AdminActivityAction;
import com.codex.flashsale.admin.AdminActivityAuditService;
import com.codex.flashsale.admin.AdminActivityOutcome;
import com.codex.flashsale.admin.AdminActivityResourceType;
import com.codex.flashsale.admin.AdminActor;
import com.codex.flashsale.admin.AdminRequestMetadata;
import com.codex.flashsale.api.OpsAlertResponse;
import com.codex.flashsale.api.OutboxBacklogResponse;
import com.codex.flashsale.api.OutboxEventSummaryResponse;
import com.codex.flashsale.api.OutboxRetryResponse;
import com.codex.flashsale.api.ReconciliationDriftResponse;
import com.codex.flashsale.api.ReconciliationRunResponse;
import com.codex.flashsale.api.ResolveReconciliationDriftRequest;
import com.codex.flashsale.application.OpsApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.codex.flashsale.outbox.OutboxStatus;

@RestController
@RequestMapping("/api/v1/admin/ops")
public class AdminOpsController {

    private final OpsApplicationService opsApplicationService;
    private final AdminActivityAuditService adminActivityAuditService;

    public AdminOpsController(
            OpsApplicationService opsApplicationService,
            AdminActivityAuditService adminActivityAuditService
    ) {
        this.opsApplicationService = opsApplicationService;
        this.adminActivityAuditService = adminActivityAuditService;
    }

    @GetMapping("/alerts")
    public List<OpsAlertResponse> getAlerts() {
        return opsApplicationService.getAlerts();
    }

    @GetMapping("/outbox/backlog")
    public OutboxBacklogResponse getOutboxBacklog() {
        return opsApplicationService.getOutboxBacklog();
    }

    @GetMapping("/outbox/events")
    public List<OutboxEventSummaryResponse> listOutboxEvents(
            @RequestParam(defaultValue = "FAILED") OutboxStatus status,
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        return opsApplicationService.listOutboxEvents(status, limit);
    }

    @PostMapping("/outbox/{eventId}/retry")
    public OutboxRetryResponse retryOutboxEvent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String eventId,
            HttpServletRequest servletRequest
    ) {
        OutboxRetryResponse response = opsApplicationService.retryOutboxEvent(eventId);
        record(jwt, AdminActivityAction.OUTBOX_RETRY_TRIGGERED, eventId, servletRequest, "status=%s".formatted(response.status()));
        return response;
    }

    @PostMapping("/reconciliation/runs")
    public ReconciliationRunResponse runReconciliation(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest servletRequest
    ) {
        ReconciliationRunResponse response = opsApplicationService.runReconciliation();
        record(jwt, AdminActivityAction.RECONCILIATION_RUN_TRIGGERED, response.runId(), servletRequest, "status=%s".formatted(response.status()));
        return response;
    }

    @GetMapping("/reconciliation/runs")
    public List<ReconciliationRunResponse> listReconciliationRuns(
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        return opsApplicationService.listReconciliationRuns(limit);
    }

    @GetMapping("/reconciliation/drifts")
    public List<ReconciliationDriftResponse> getOpenReconciliationDrifts() {
        return opsApplicationService.listOpenReconciliationDrifts();
    }

    @PostMapping("/reconciliation/{driftId}/resolve")
    public ReconciliationDriftResponse resolveDrift(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String driftId,
            @Valid @RequestBody ResolveReconciliationDriftRequest request,
            HttpServletRequest servletRequest
    ) {
        ReconciliationDriftResponse response = opsApplicationService.resolveReconciliationDrift(driftId, request.resolutionNote());
        record(jwt, AdminActivityAction.RECONCILIATION_DRIFT_RESOLVED, driftId, servletRequest, request.resolutionNote());
        return response;
    }

    private void record(
            Jwt jwt,
            AdminActivityAction action,
            String resourceId,
            HttpServletRequest request,
            String details
    ) {
        AdminActor actor = AdminActor.from(jwt);
        AdminRequestMetadata metadata = AdminRequestMetadata.from(request);
        adminActivityAuditService.record(
                actor.username(),
                actor.role(),
                action,
                AdminActivityResourceType.OPS,
                resourceId,
                AdminActivityOutcome.SUCCESS,
                metadata.correlationId(),
                details + ", " + metadata.asDetail()
        );
    }
}
