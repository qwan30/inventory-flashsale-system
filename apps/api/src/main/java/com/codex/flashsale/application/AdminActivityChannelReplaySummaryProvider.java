package com.codex.flashsale.application;

import com.codex.flashsale.admin.AdminActivityAction;
import com.codex.flashsale.admin.AdminActivityAuditRepository;
import com.codex.flashsale.common.domain.SalesChannel;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AdminActivityChannelReplaySummaryProvider implements ChannelReplaySummaryProvider {

    private final AdminActivityAuditRepository adminActivityAuditRepository;

    public AdminActivityChannelReplaySummaryProvider(AdminActivityAuditRepository adminActivityAuditRepository) {
        this.adminActivityAuditRepository = adminActivityAuditRepository;
    }

    @Override
    public Optional<ChannelReplaySummary> findLatest(SalesChannel channel) {
        if (channel != SalesChannel.TIKTOK_SHOP) {
            return Optional.empty();
        }
        return adminActivityAuditRepository.findTopByActionOrderByCreatedAtDesc(
                        AdminActivityAction.TIKTOK_INGRESS_REPLAY_TRIGGERED
                )
                .map(audit -> new ChannelReplaySummary(
                        audit.getAction().name(),
                        audit.getResourceId(),
                        audit.getOutcome().name(),
                        audit.getCreatedAt(),
                        audit.getDetails()
                ));
    }
}
