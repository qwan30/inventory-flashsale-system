package com.codex.flashsale.controller;

import com.codex.flashsale.admin.AdminActivityAction;
import com.codex.flashsale.admin.AdminActivityAuditService;
import com.codex.flashsale.admin.AdminActivityOutcome;
import com.codex.flashsale.admin.AdminActivityResourceType;
import com.codex.flashsale.admin.AdminActor;
import com.codex.flashsale.admin.AdminRequestMetadata;
import com.codex.flashsale.ai.OpsCopilotService;
import com.codex.flashsale.api.OpsCopilotAnalyzeRequest;
import com.codex.flashsale.api.OpsCopilotAnalyzeResponse;
import com.codex.flashsale.api.OpsCopilotCapabilitiesResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ops/copilot")
public class AdminOpsCopilotController {

    private final OpsCopilotService opsCopilotService;
    private final AdminActivityAuditService adminActivityAuditService;

    public AdminOpsCopilotController(
            OpsCopilotService opsCopilotService,
            AdminActivityAuditService adminActivityAuditService
    ) {
        this.opsCopilotService = opsCopilotService;
        this.adminActivityAuditService = adminActivityAuditService;
    }

    @GetMapping("/capabilities")
    public OpsCopilotCapabilitiesResponse capabilities() {
        return opsCopilotService.getCapabilities();
    }

    @PostMapping("/analyze")
    public OpsCopilotAnalyzeResponse analyze(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OpsCopilotAnalyzeRequest request,
            HttpServletRequest servletRequest
    ) {
        AdminActor actor = AdminActor.from(jwt);
        AdminRequestMetadata metadata = AdminRequestMetadata.from(servletRequest);
        try {
            OpsCopilotAnalyzeResponse response = opsCopilotService.analyze(request);
            record(
                    actor,
                    request.scope(),
                    response.subjectId(),
                    response.providerMetadata().requestId(),
                    response.providerMetadata().provider(),
                    response.providerMetadata().model(),
                    metadata,
                    AdminActivityOutcome.SUCCESS
            );
            return response;
        } catch (RuntimeException exception) {
            record(
                    actor,
                    request.scope(),
                    subjectId(request),
                    null,
                    "unavailable",
                    "unavailable",
                    metadata,
                    AdminActivityOutcome.FAILURE
            );
            throw exception;
        }
    }

    private void record(
            AdminActor actor,
            String scope,
            String subjectId,
            String requestId,
            String provider,
            String model,
            AdminRequestMetadata metadata,
            AdminActivityOutcome outcome
    ) {
        String details = "scope=%s, subject=%s, provider=%s, model=%s, requestId=%s, %s"
                .formatted(scope, subjectId, provider, model, requestId, metadata.asDetail());
        adminActivityAuditService.record(
                actor.username(),
                actor.role(),
                AdminActivityAction.OPS_COPILOT_ANALYSIS_REQUESTED,
                AdminActivityResourceType.OPS,
                "%s:%s".formatted(scope, subjectId),
                outcome,
                metadata.correlationId(),
                details
        );
    }

    private String subjectId(OpsCopilotAnalyzeRequest request) {
        if (request.benchmarkRunId() != null && !request.benchmarkRunId().isBlank()) {
            return request.benchmarkRunId();
        }
        if (request.channel() != null && !request.channel().isBlank()) {
            return request.channel();
        }
        if (request.campaignId() != null && !request.campaignId().isBlank()) {
            return request.campaignId();
        }
        return "ops-overview";
    }
}
