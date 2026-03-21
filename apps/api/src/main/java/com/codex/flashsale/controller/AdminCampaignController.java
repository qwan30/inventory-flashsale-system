package com.codex.flashsale.controller;

import com.codex.flashsale.admin.AdminActor;
import com.codex.flashsale.admin.AdminRequestMetadata;
import com.codex.flashsale.api.AdminActivityResponse;
import com.codex.flashsale.api.AdminCampaignResponse;
import com.codex.flashsale.api.AdminCreateCampaignRequest;
import com.codex.flashsale.api.AdminUpdateCampaignRequest;
import com.codex.flashsale.application.AdminCampaignApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/campaigns")
public class AdminCampaignController {

    private final AdminCampaignApplicationService adminCampaignApplicationService;

    public AdminCampaignController(AdminCampaignApplicationService adminCampaignApplicationService) {
        this.adminCampaignApplicationService = adminCampaignApplicationService;
    }

    @GetMapping
    public List<AdminCampaignResponse> listCampaigns() {
        return adminCampaignApplicationService.listCampaigns();
    }

    @GetMapping("/{campaignId}")
    public AdminCampaignResponse getCampaign(@PathVariable String campaignId) {
        return adminCampaignApplicationService.getCampaign(campaignId);
    }

    @PostMapping
    public AdminCampaignResponse createCampaign(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminCreateCampaignRequest request,
            HttpServletRequest servletRequest
    ) {
        return adminCampaignApplicationService.createCampaign(
                request,
                AdminActor.from(jwt),
                AdminRequestMetadata.from(servletRequest).correlationId()
        );
    }

    @PutMapping("/{campaignId}")
    public AdminCampaignResponse updateCampaign(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String campaignId,
            @Valid @RequestBody AdminUpdateCampaignRequest request,
            HttpServletRequest servletRequest
    ) {
        return adminCampaignApplicationService.updateCampaign(
                campaignId,
                request,
                AdminActor.from(jwt),
                AdminRequestMetadata.from(servletRequest).correlationId()
        );
    }

    @PostMapping("/{campaignId}/activate")
    public AdminCampaignResponse activateCampaign(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String campaignId,
            HttpServletRequest servletRequest
    ) {
        return adminCampaignApplicationService.activateCampaign(
                campaignId,
                AdminActor.from(jwt),
                AdminRequestMetadata.from(servletRequest).correlationId()
        );
    }

    @PostMapping("/{campaignId}/end")
    public AdminCampaignResponse endCampaign(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String campaignId,
            HttpServletRequest servletRequest
    ) {
        return adminCampaignApplicationService.endCampaign(
                campaignId,
                AdminActor.from(jwt),
                AdminRequestMetadata.from(servletRequest).correlationId()
        );
    }

    @GetMapping("/{campaignId}/audits")
    public List<AdminActivityResponse> getCampaignActivity(@PathVariable String campaignId) {
        return adminCampaignApplicationService.getCampaignActivity(campaignId);
    }
}
