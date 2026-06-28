package com.codex.flashsale.api;

import com.codex.flashsale.common.domain.SalesChannel;
import com.codex.flashsale.inventory.ReservationStatus;
import java.time.Instant;

public record ReservationResponse(
        String reservationId,
        String campaignId,
        String sku,
        SalesChannel channel,
        int quantity,
        ReservationStatus status,
        Instant expiresAt,
        InventoryResponse inventory,
        int remainingCampaignQty
) {
}

