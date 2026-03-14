package com.codex.flashsale.channel;

public interface ChannelAdapter {

    SalesChannel channel();

    void validateReservation(ReservationValidationRequest request);
}

