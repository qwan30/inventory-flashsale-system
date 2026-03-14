package com.codex.flashsale.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.exception.ConflictException;
import org.junit.jupiter.api.Test;

class OrderHeaderTest {

    @Test
    void shouldAdvanceSequentially() {
        OrderHeader order = new OrderHeader("ord-1", "res-1", SalesChannel.WEB, OrderStatus.PENDING);

        order.transitionTo(OrderStatus.PAID);
        order.transitionTo(OrderStatus.SHIPPED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void shouldRejectInvalidTransition() {
        OrderHeader order = new OrderHeader("ord-1", "res-1", SalesChannel.WEB, OrderStatus.PENDING);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.SHIPPED))
                .isInstanceOf(ConflictException.class);
    }
}
