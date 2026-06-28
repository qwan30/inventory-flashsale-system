package com.codex.flashsale.channel;

import com.codex.flashsale.common.domain.SalesChannel;

public record ReservationValidationRequest(
        SalesChannel channel,
        String sku,
        int quantity
) {
}

