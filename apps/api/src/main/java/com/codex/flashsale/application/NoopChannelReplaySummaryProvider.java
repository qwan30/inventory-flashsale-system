package com.codex.flashsale.application;

import com.codex.flashsale.common.domain.SalesChannel;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(ChannelReplaySummaryProvider.class)
public class NoopChannelReplaySummaryProvider implements ChannelReplaySummaryProvider {

    @Override
    public Optional<ChannelReplaySummary> findLatest(SalesChannel channel) {
        return Optional.empty();
    }
}
