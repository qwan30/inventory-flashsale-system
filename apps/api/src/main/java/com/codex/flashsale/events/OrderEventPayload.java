package com.codex.flashsale.events;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.order.OrderStatus;

public record OrderEventPayload(
        String orderId,
        String reservationId,
        SalesChannel channel,
        OrderStatus status
) {
}

