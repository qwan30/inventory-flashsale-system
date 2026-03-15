package com.codex.flashsale.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record AdminCreateCampaignRequest(
        @NotBlank String id,
        @NotBlank String sku,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @Min(1) int quota
) {
}
