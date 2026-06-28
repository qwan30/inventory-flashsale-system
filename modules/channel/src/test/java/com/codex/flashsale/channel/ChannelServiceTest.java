package com.codex.flashsale.channel;

import com.codex.flashsale.common.domain.SalesChannel;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codex.flashsale.common.exception.BadRequestException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChannelServiceTest {

    private final ChannelService channelService = new ChannelService(List.of(
            new MockWebChannelAdapter(),
            new MockAppChannelAdapter(),
            new MockShopeeChannelAdapter()
    ));

    @Test
    void shouldValidateSupportedChannel() {
        assertThatCode(() -> channelService.validateReservation(
                new ReservationValidationRequest(SalesChannel.WEB, "SKU-1", 1)
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidQuantity() {
        assertThatThrownBy(() -> channelService.validateReservation(
                new ReservationValidationRequest(SalesChannel.APP, "SKU-1", 0)
        )).isInstanceOf(BadRequestException.class);
    }
}
