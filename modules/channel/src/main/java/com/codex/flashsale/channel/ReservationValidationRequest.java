package com.codex.flashsale.channel;

public record ReservationValidationRequest(
        SalesChannel channel,
        String sku,
        int quantity
) {
}

