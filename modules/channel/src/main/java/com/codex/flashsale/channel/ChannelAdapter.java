package com.codex.flashsale.channel;

import com.codex.flashsale.common.domain.SalesChannel;

public interface ChannelAdapter {

    SalesChannel channel();

    void validateReservation(ReservationValidationRequest request);
}

