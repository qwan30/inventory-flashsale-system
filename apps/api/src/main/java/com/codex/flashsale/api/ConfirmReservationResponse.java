package com.codex.flashsale.api;

import com.codex.flashsale.order.OrderStatus;

public record ConfirmReservationResponse(
        String reservationId,
        String orderId,
        OrderStatus orderStatus
) {
}

