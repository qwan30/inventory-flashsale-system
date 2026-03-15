package com.codex.flashsale.api;

import com.codex.flashsale.flashsale.CampaignStatus;
import java.time.Instant;

public record AdminCampaignResponse(
        String id,
        String sku,
        Instant startsAt,
        Instant endsAt,
        int quota,
        int reservedQuota,
        int soldQuota,
        CampaignStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
