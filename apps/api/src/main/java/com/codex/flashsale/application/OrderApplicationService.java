package com.codex.flashsale.application;

import com.codex.flashsale.api.OrderResponse;
import com.codex.flashsale.channel.sync.ChannelSyncService;
import com.codex.flashsale.events.EventContract;
import com.codex.flashsale.events.EventContracts;
import com.codex.flashsale.events.OrderEventPayload;
import com.codex.flashsale.idempotency.OperationIdempotencyService;
import com.codex.flashsale.idempotency.OperationIdempotencyType;
import com.codex.flashsale.common.exception.ConflictException;
import com.codex.flashsale.order.OrderHeader;
import com.codex.flashsale.order.OrderService;
import com.codex.flashsale.order.OrderStatus;
import com.codex.flashsale.outbox.OutboxEvent;
import com.codex.flashsale.outbox.OutboxService;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderApplicationService {

    private final OrderService orderService;
    private final OutboxService outboxService;
    private final ChannelSyncService channelSyncService;
    private final OperationIdempotencyService operationIdempotencyService;
    private final TransactionTemplate transactionTemplate;

    public OrderApplicationService(
            OrderService orderService,
            OutboxService outboxService,
            ChannelSyncService channelSyncService,
            OperationIdempotencyService operationIdempotencyService,
            PlatformTransactionManager transactionManager
    ) {
        this.orderService = orderService;
        this.outboxService = outboxService;
        this.channelSyncService = channelSyncService;
        this.operationIdempotencyService = operationIdempotencyService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public OrderResponse updateStatus(String orderId, OrderStatus newStatus, String idempotencyKey) {
        if (hasIdempotencyKey(idempotencyKey)) {
            OrderResponse recordedResponse = findRecordedOrderResponse(orderId, idempotencyKey);
            if (recordedResponse != null) {
                return recordedResponse;
            }
            OrderHeader existingOrder = orderService.getRequired(orderId);
            if (existingOrder.getStatus() == newStatus
                    || operationIdempotencyService.hasRecord(
                    OperationIdempotencyType.ORDER_STATUS_UPDATE,
                    orderId,
                    newStatus.name()
            )) {
                throw duplicateStatusConflict(orderId, newStatus);
            }
        }

        try {
            return transactionTemplate.execute(status -> {
            OrderHeader order = orderService.getRequired(orderId);
            if (!hasIdempotencyKey(idempotencyKey) && order.getStatus() == newStatus) {
                return toOrderResponse(order);
            }
            order.transitionTo(newStatus);
            orderService.save(order);

            OrderResponse response = toOrderResponse(order);
            if (hasIdempotencyKey(idempotencyKey)) {
                operationIdempotencyService.record(
                        OperationIdempotencyType.ORDER_STATUS_UPDATE,
                        order.getId(),
                        newStatus.name(),
                        idempotencyKey,
                        response
                );
            }

            EventContract contract = switch (newStatus) {
                case PAID -> EventContracts.ORDER_PAID;
                case SHIPPED -> EventContracts.ORDER_SHIPPED;
                default -> EventContracts.ORDER_CREATED;
            };
            OutboxEvent outboxEvent = outboxService.record(
                    "order",
                    order.getId(),
                    contract.eventType(),
                    contract.version(),
                    new OrderEventPayload(order.getId(), order.getReservationId(), order.getChannel(), order.getStatus())
            );
            channelSyncService.scheduleSync(
                    outboxEvent.getId(),
                    outboxEvent.getEventType(),
                    outboxEvent.getPayload(),
                    Set.of(order.getChannel()),
                    null,
                    null,
                    null,
                    null
            );
            return response;
            });
        } catch (DataIntegrityViolationException exception) {
            OrderResponse recordedResponse = findRecordedOrderResponse(orderId, idempotencyKey);
            if (recordedResponse != null) {
                return recordedResponse;
            }
            throw duplicateStatusConflict(orderId, newStatus);
        }
    }

    private OrderResponse toOrderResponse(OrderHeader order) {
        return new OrderResponse(order.getId(), order.getReservationId(), order.getChannel(), order.getStatus());
    }

    private OrderResponse findRecordedOrderResponse(String orderId, String idempotencyKey) {
        if (!hasIdempotencyKey(idempotencyKey)) {
            return null;
        }
        return operationIdempotencyService.findRecordedResponse(
                OperationIdempotencyType.ORDER_STATUS_UPDATE,
                orderId,
                idempotencyKey,
                OrderResponse.class
        ).orElse(null);
    }

    private ConflictException duplicateStatusConflict(String orderId, OrderStatus newStatus) {
        return new ConflictException(
                "ORDER_STATUS_ALREADY_PROCESSED",
                "Order %s already processed transition to %s with a different idempotency key".formatted(orderId, newStatus)
        );
    }

    private boolean hasIdempotencyKey(String idempotencyKey) {
        return idempotencyKey != null && !idempotencyKey.isBlank();
    }
}
