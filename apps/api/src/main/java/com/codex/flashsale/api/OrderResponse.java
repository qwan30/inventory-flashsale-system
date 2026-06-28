package com.codex.flashsale.api;

import com.codex.flashsale.common.domain.SalesChannel;
import com.codex.flashsale.order.OrderStatus;

public record OrderResponse(
        String orderId,
        String reservationId,
        SalesChannel channel,
        OrderStatus status
) {
}

