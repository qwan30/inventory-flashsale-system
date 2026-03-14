package com.codex.flashsale.application;

import com.codex.flashsale.api.OrderResponse;
import com.codex.flashsale.events.OrderEventPayload;
import com.codex.flashsale.order.OrderHeader;
import com.codex.flashsale.order.OrderService;
import com.codex.flashsale.order.OrderStatus;
import com.codex.flashsale.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderApplicationService {

    private final OrderService orderService;
    private final OutboxService outboxService;
    private final TransactionTemplate transactionTemplate;

    public OrderApplicationService(
            OrderService orderService,
            OutboxService outboxService,
            PlatformTransactionManager transactionManager
    ) {
        this.orderService = orderService;
        this.outboxService = outboxService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public OrderResponse updateStatus(String orderId, OrderStatus newStatus) {
        return transactionTemplate.execute(status -> {
            OrderHeader order = orderService.getRequired(orderId);
            order.transitionTo(newStatus);
            orderService.save(order);

            String eventType = switch (newStatus) {
                case PAID -> "order.paid";
                case SHIPPED -> "order.shipped";
                default -> "order.created";
            };
            outboxService.record(
                    "order",
                    order.getId(),
                    eventType,
                    new OrderEventPayload(order.getId(), order.getReservationId(), order.getChannel(), order.getStatus())
            );
            return new OrderResponse(order.getId(), order.getReservationId(), order.getChannel(), order.getStatus());
        });
    }
}
