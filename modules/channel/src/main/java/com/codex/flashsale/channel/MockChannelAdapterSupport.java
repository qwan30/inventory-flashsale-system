package com.codex.flashsale.channel;

import com.codex.flashsale.common.exception.BadRequestException;

abstract class MockChannelAdapterSupport implements ChannelAdapter {

    @Override
    public void validateReservation(ReservationValidationRequest request) {
        if (request.quantity() <= 0) {
            throw new BadRequestException("INVALID_QUANTITY", "Quantity must be greater than zero");
        }
        if (request.sku() == null || request.sku().isBlank()) {
            throw new BadRequestException("INVALID_SKU", "SKU must not be blank");
        }
    }
}

