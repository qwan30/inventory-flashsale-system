package com.codex.flashsale.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record AdminUpdateCampaignRequest(
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @Min(1) int quota
) {
}
