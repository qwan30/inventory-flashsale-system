package com.codex.flashsale.order;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.exception.NotFoundException;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderHeaderRepository repository;

    public OrderService(OrderHeaderRepository repository) {
        this.repository = repository;
    }

    public OrderHeader createPendingOrder(String orderId, String reservationId, SalesChannel channel) {
        return repository.saveAndFlush(new OrderHeader(orderId, reservationId, channel, OrderStatus.PENDING));
    }

    public OrderHeader getRequired(String orderId) {
        return repository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found: " + orderId));
    }

    public Optional<OrderHeader> findByReservationId(String reservationId) {
        return repository.findByReservationId(reservationId);
    }

    public OrderHeader save(OrderHeader orderHeader) {
        return repository.saveAndFlush(orderHeader);
    }
}

