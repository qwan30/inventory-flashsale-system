package com.codex.flashsale.order;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.exception.ConflictException;
import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_header")
public class OrderHeader extends AuditTimestamps {

    @Id
    private String id;

    @Column(name = "reservation_id", nullable = false)
    private String reservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private SalesChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    protected OrderHeader() {
    }

    public OrderHeader(String id, String reservationId, SalesChannel channel, OrderStatus status) {
        this.id = id;
        this.reservationId = reservationId;
        this.channel = channel;
        this.status = status;
    }

    public void transitionTo(OrderStatus newStatus) {
        if (status == newStatus) {
            return;
        }
        if (status == OrderStatus.PENDING && newStatus == OrderStatus.PAID) {
            status = newStatus;
            return;
        }
        if (status == OrderStatus.PAID && newStatus == OrderStatus.SHIPPED) {
            status = newStatus;
            return;
        }
        throw new ConflictException(
                "ORDER_STATUS_TRANSITION_INVALID",
                "Invalid order transition from %s to %s".formatted(status, newStatus)
        );
    }

    public String getId() {
        return id;
    }

    public String getReservationId() {
        return reservationId;
    }

    public SalesChannel getChannel() {
        return channel;
    }

    public OrderStatus getStatus() {
        return status;
    }
}

