package com.codex.flashsale.events;

import com.codex.flashsale.common.domain.SalesChannel;
import com.codex.flashsale.inventory.ReservationStatus;
import java.time.Instant;

public record ReservationEventPayload(
        String reservationId,
        String campaignId,
        String sku,
        SalesChannel channel,
        int quantity,
        ReservationStatus status,
        Instant expiresAt
) {
}
