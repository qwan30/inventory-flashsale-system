package com.codex.flashsale.channel;

import com.codex.flashsale.common.exception.BadRequestException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChannelService {

    private final Map<SalesChannel, ChannelAdapter> adapters;

    public ChannelService(List<ChannelAdapter> adapters) {
        this.adapters = new EnumMap<>(SalesChannel.class);
        adapters.forEach(adapter -> this.adapters.put(adapter.channel(), adapter));
    }

    public void validateReservation(ReservationValidationRequest request) {
        ChannelAdapter adapter = adapters.get(request.channel());
        if (adapter == null) {
            throw new BadRequestException("CHANNEL_NOT_SUPPORTED", "Unsupported sales channel: " + request.channel());
        }
        adapter.validateReservation(request);
    }
}

