package com.codex.flashsale.application;

import com.codex.flashsale.common.domain.SalesChannel;
import java.util.Optional;

public interface ChannelReplaySummaryProvider {

    Optional<ChannelReplaySummary> findLatest(SalesChannel channel);
}
