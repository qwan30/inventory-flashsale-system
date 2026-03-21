package com.codex.flashsale.events;

import com.codex.flashsale.outbox.OutboxEvent;

public final class EventContracts {

    public static final EventContract RESERVATION_CREATED =
            new EventContract("inventory.reservation.created", OutboxEvent.DEFAULT_EVENT_VERSION);
    public static final EventContract RESERVATION_RELEASED =
            new EventContract("inventory.reservation.released", OutboxEvent.DEFAULT_EVENT_VERSION);
    public static final EventContract ORDER_CREATED =
            new EventContract("order.created", OutboxEvent.DEFAULT_EVENT_VERSION);
    public static final EventContract ORDER_PAID =
            new EventContract("order.paid", OutboxEvent.DEFAULT_EVENT_VERSION);
    public static final EventContract ORDER_SHIPPED =
            new EventContract("order.shipped", OutboxEvent.DEFAULT_EVENT_VERSION);

    private EventContracts() {
    }
}
