package com.codex.flashsale.order;

import com.codex.flashsale.common.domain.SalesChannel;
import com.codex.flashsale.common.exception.ConflictException;
import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents the header/metadata of a customer order.
 * Orders start in {@code PENDING} and transition to either {@code PAID} or {@code FAILED}
 * based on checkout results, before potentially moving to {@code SHIPPED}.
 */
@Entity
@Table(name = "order_header")
public class OrderHeader extends AuditTimestamps {

    @Id
    private String id;

    /** The ID of the stock reservation that guarantees the inventory for this order. */
    @Column(name = "reservation_id", nullable = false)
    private String reservationId;

    /** The sales channel from which the order was placed. */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private SalesChannel channel;

    /** The current state of the order: {@code PENDING}, {@code PAID}, {@code SHIPPED}, {@code FAILED}. */
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

