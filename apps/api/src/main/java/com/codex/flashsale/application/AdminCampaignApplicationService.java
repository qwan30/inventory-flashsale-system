package com.codex.flashsale.application;

import com.codex.flashsale.admin.AdminActivityAction;
import com.codex.flashsale.admin.AdminActivityAudit;
import com.codex.flashsale.admin.AdminActivityAuditService;
import com.codex.flashsale.admin.AdminActivityOutcome;
import com.codex.flashsale.admin.AdminActivityResourceType;
import com.codex.flashsale.admin.AdminActor;
import com.codex.flashsale.api.AdminActivityResponse;
import com.codex.flashsale.api.AdminCampaignResponse;
import com.codex.flashsale.api.AdminCreateCampaignRequest;
import com.codex.flashsale.api.AdminUpdateCampaignRequest;
import com.codex.flashsale.common.exception.ConflictException;
import com.codex.flashsale.flashsale.FlashSaleCampaign;
import com.codex.flashsale.flashsale.FlashSaleCampaignService;
import com.codex.flashsale.inventory.InventoryService;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCampaignApplicationService {

    private final FlashSaleCampaignService flashSaleCampaignService;
    private final InventoryService inventoryService;
    private final AdminActivityAuditService adminActivityAuditService;

    public AdminCampaignApplicationService(
            FlashSaleCampaignService flashSaleCampaignService,
            InventoryService inventoryService,
            AdminActivityAuditService adminActivityAuditService
    ) {
        this.flashSaleCampaignService = flashSaleCampaignService;
        this.inventoryService = inventoryService;
        this.adminActivityAuditService = adminActivityAuditService;
    }

    @Transactional(readOnly = true)
    public List<AdminCampaignResponse> listCampaigns() {
        return flashSaleCampaignService.findAll().stream()
                .sorted(Comparator.comparing(FlashSaleCampaign::getStartsAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminCampaignResponse getCampaign(String campaignId) {
        return toResponse(flashSaleCampaignService.getRequired(campaignId));
    }

    @Transactional
    public AdminCampaignResponse createCampaign(AdminCreateCampaignRequest request, AdminActor actor, String correlationId) {
        if (flashSaleCampaignService.exists(request.id())) {
            throw new ConflictException("FLASH_SALE_CAMPAIGN_ALREADY_EXISTS", "Campaign already exists: " + request.id());
        }
        inventoryService.getRequiredInventory(request.sku());
        FlashSaleCampaign campaign = flashSaleCampaignService.save(FlashSaleCampaign.draft(
                request.id(),
                request.sku(),
                request.startsAt(),
                request.endsAt(),
                request.quota()
        ));
        adminActivityAuditService.record(
                actor.username(),
                actor.role(),
                AdminActivityAction.CAMPAIGN_CREATED,
                AdminActivityResourceType.CAMPAIGN,
                campaign.getId(),
                AdminActivityOutcome.SUCCESS,
                correlationId,
                "sku=%s".formatted(campaign.getSku())
        );
        return toResponse(campaign);
    }

    @Transactional
    public AdminCampaignResponse updateCampaign(
            String campaignId,
            AdminUpdateCampaignRequest request,
            AdminActor actor,
            String correlationId
    ) {
        FlashSaleCampaign campaign = flashSaleCampaignService.getRequired(campaignId);
        campaign.updateDraft(request.startsAt(), request.endsAt(), request.quota());
        flashSaleCampaignService.save(campaign);
        adminActivityAuditService.record(
                actor.username(),
                actor.role(),
                AdminActivityAction.CAMPAIGN_UPDATED,
                AdminActivityResourceType.CAMPAIGN,
                campaign.getId(),
                AdminActivityOutcome.SUCCESS,
                correlationId,
                "quota=%s".formatted(campaign.getQuota())
        );
        return toResponse(campaign);
    }

    @Transactional
    public AdminCampaignResponse activateCampaign(String campaignId, AdminActor actor, String correlationId) {
        FlashSaleCampaign campaign = flashSaleCampaignService.getRequired(campaignId);
        campaign.activate();
        flashSaleCampaignService.save(campaign);
        adminActivityAuditService.record(
                actor.username(),
                actor.role(),
                AdminActivityAction.CAMPAIGN_ACTIVATED,
                AdminActivityResourceType.CAMPAIGN,
                campaign.getId(),
                AdminActivityOutcome.SUCCESS,
                correlationId,
                "status=%s".formatted(campaign.getStatus())
        );
        return toResponse(campaign);
    }

    @Transactional
    public AdminCampaignResponse endCampaign(String campaignId, AdminActor actor, String correlationId) {
        FlashSaleCampaign campaign = flashSaleCampaignService.getRequired(campaignId);
        campaign.end();
        flashSaleCampaignService.save(campaign);
        adminActivityAuditService.record(
                actor.username(),
                actor.role(),
                AdminActivityAction.CAMPAIGN_ENDED,
                AdminActivityResourceType.CAMPAIGN,
                campaign.getId(),
                AdminActivityOutcome.SUCCESS,
                correlationId,
                "status=%s".formatted(campaign.getStatus())
        );
        return toResponse(campaign);
    }

    @Transactional(readOnly = true)
    public List<AdminActivityResponse> getCampaignActivity(String campaignId) {
        flashSaleCampaignService.getRequired(campaignId);
        return adminActivityAuditService.findResourceActivity(AdminActivityResourceType.CAMPAIGN, campaignId)
                .stream()
                .map(this::toActivityResponse)
                .toList();
    }

    private AdminCampaignResponse toResponse(FlashSaleCampaign campaign) {
        return new AdminCampaignResponse(
                campaign.getId(),
                campaign.getSku(),
                campaign.getStartsAt(),
                campaign.getEndsAt(),
                campaign.getQuota(),
                campaign.getReservedQuota(),
                campaign.getSoldQuota(),
                campaign.getStatus(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }

    private AdminActivityResponse toActivityResponse(AdminActivityAudit audit) {
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
