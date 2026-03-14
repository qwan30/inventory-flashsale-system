package com.codex.flashsale.controller;

import com.codex.flashsale.api.OpsAlertResponse;
import com.codex.flashsale.api.OutboxBacklogResponse;
import com.codex.flashsale.api.OutboxRetryResponse;
import com.codex.flashsale.api.ReconciliationDriftResponse;
import com.codex.flashsale.api.ReconciliationRunResponse;
import com.codex.flashsale.api.ResolveReconciliationDriftRequest;
import com.codex.flashsale.application.OpsApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops")
public class OpsController {

    private final OpsApplicationService opsApplicationService;

    public OpsController(OpsApplicationService opsApplicationService) {
        this.opsApplicationService = opsApplicationService;
    }

    @GetMapping("/outbox/backlog")
    public OutboxBacklogResponse getOutboxBacklog() {
        return opsApplicationService.getOutboxBacklog();
    }

    @GetMapping("/alerts")
    public List<OpsAlertResponse> getAlerts() {
        return opsApplicationService.getAlerts();
    }

    @PostMapping("/outbox/{eventId}/retry")
    public OutboxRetryResponse retryOutboxEvent(@PathVariable String eventId) {
        return opsApplicationService.retryOutboxEvent(eventId);
    }

    @PostMapping("/reconciliation/runs")
    public ReconciliationRunResponse runReconciliation() {
        return opsApplicationService.runReconciliation();
    }

    @GetMapping("/reconciliation/drifts")
    public List<ReconciliationDriftResponse> getOpenReconciliationDrifts() {
        return opsApplicationService.listOpenReconciliationDrifts();
    }

    @PostMapping("/reconciliation/{driftId}/resolve")
    public ReconciliationDriftResponse resolveDrift(
            @PathVariable String driftId,
            @Valid @RequestBody ResolveReconciliationDriftRequest request
    ) {
        return opsApplicationService.resolveReconciliationDrift(driftId, request.resolutionNote());
    }
}
