package com.codex.flashsale.flashsale;

import com.codex.flashsale.common.exception.BadRequestException;
import com.codex.flashsale.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FlashSaleCampaignService {

    private final FlashSaleCampaignRepository repository;

    public FlashSaleCampaignService(FlashSaleCampaignRepository repository) {
        this.repository = repository;
    }

    public FlashSaleCampaign getRequired(String campaignId) {
        return repository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("CAMPAIGN_NOT_FOUND", "Flash sale campaign not found: " + campaignId));
    }

    public void validateCampaignSku(FlashSaleCampaign campaign, String sku) {
        if (!campaign.getSku().equals(sku)) {
            throw new BadRequestException("CAMPAIGN_SKU_MISMATCH", "Campaign SKU does not match requested SKU");
        }
    }

    public void ensureReservable(FlashSaleCampaign campaign, int quantity, Instant now) {
        campaign.ensureActive(now);
        campaign.reserveQuota(quantity);
    }

    public FlashSaleCampaign save(FlashSaleCampaign campaign) {
        return repository.save(campaign);
    }

    public List<FlashSaleCampaign> findAll() {
        return repository.findAll();
    }

    public boolean exists(String campaignId) {
        return repository.existsById(campaignId);
    }
}
