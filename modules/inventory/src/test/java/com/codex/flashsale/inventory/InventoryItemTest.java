package com.codex.flashsale.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codex.flashsale.common.exception.ConflictException;
import org.junit.jupiter.api.Test;

class InventoryItemTest {

    @Test
    void shouldReserveReleaseAndConfirm() {
        InventoryItem item = new InventoryItem("SKU-1", 10, 0, 0);

        item.reserve(4);
        item.release(1);
        item.confirm(3);

        assertThat(item.getAvailableQty()).isEqualTo(7);
        assertThat(item.getReservedQty()).isZero();
        assertThat(item.getSoldQty()).isEqualTo(3);
    }

    @Test
    void shouldRejectInsufficientStock() {
        InventoryItem item = new InventoryItem("SKU-1", 1, 0, 0);

        assertThatThrownBy(() -> item.reserve(2)).isInstanceOf(ConflictException.class);
    }
}
