package com.codex.flashsale.flashsale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codex.flashsale.common.exception.ConflictException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FlashSaleCampaignTest {

    @Test
    void shouldReserveAndConfirmQuota() {
        FlashSaleCampaign campaign = new FlashSaleCampaign(
                "cmp-1",
                "SKU-1",
                Instant.parse("2026-03-14T00:00:00Z"),
                Instant.parse("2026-03-15T00:00:00Z"),
                10,
                0,
                0,
                CampaignStatus.ACTIVE
        );

        campaign.ensureActive(Instant.parse("2026-03-14T10:00:00Z"));
        campaign.reserveQuota(3);
        campaign.confirmQuota(2);

        assertThat(campaign.getReservedQuota()).isEqualTo(1);
        assertThat(campaign.getSoldQuota()).isEqualTo(2);
    }

    @Test
    void shouldRejectOutOfWindowCampaign() {
        FlashSaleCampaign campaign = new FlashSaleCampaign(
                "cmp-1",
                "SKU-1",
                Instant.parse("2026-03-14T00:00:00Z"),
                Instant.parse("2026-03-14T01:00:00Z"),
                10,
                0,
                0,
                CampaignStatus.ACTIVE
        );

        assertThatThrownBy(() -> campaign.ensureActive(Instant.parse("2026-03-14T02:00:00Z")))
                .isInstanceOf(ConflictException.class);
    }
}
