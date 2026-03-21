package com.codex.flashsale.controller;

import com.codex.flashsale.admin.AdminActivityAction;
import com.codex.flashsale.admin.AdminActivityAuditService;
import com.codex.flashsale.admin.AdminActivityOutcome;
import com.codex.flashsale.admin.AdminActivityResourceType;
import com.codex.flashsale.admin.AdminActor;
import com.codex.flashsale.admin.AdminRequestMetadata;
import com.codex.flashsale.api.AdminTikTokIngressReplayRequest;
import com.codex.flashsale.api.TikTokIngressReceiptResponse;
import com.codex.flashsale.channel.ingress.TikTokIngressService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/channels/tiktok/ingress")
public class AdminTikTokIngressController {

    private final TikTokIngressService tikTokIngressService;
    private final AdminActivityAuditService adminActivityAuditService;

    public AdminTikTokIngressController(
            TikTokIngressService tikTokIngressService,
            AdminActivityAuditService adminActivityAuditService
    ) {
        this.tikTokIngressService = tikTokIngressService;
        this.adminActivityAuditService = adminActivityAuditService;
    }

    @PostMapping("/replay")
    public TikTokIngressReceiptResponse replay(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody AdminTikTokIngressReplayRequest request,
            HttpServletRequest servletRequest
    ) {
        TikTokIngressReceiptResponse response = tikTokIngressService.replay(request);
        AdminActor actor = AdminActor.from(jwt);
        AdminRequestMetadata metadata = AdminRequestMetadata.from(servletRequest);
        adminActivityAuditService.record(
                actor.username(),
                actor.role(),
                AdminActivityAction.TIKTOK_INGRESS_REPLAY_TRIGGERED,
                AdminActivityResourceType.OPS,
                request.receiptId(),
                AdminActivityOutcome.SUCCESS,
                metadata.correlationId(),
                "kind=%s, outcome=%s, detail=%s, %s".formatted(
                        request.kind(),
                        response.outcome(),
                        response.detail(),
                        metadata.asDetail()
                )
        );
        return response;
    }
}
