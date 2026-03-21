package com.codex.flashsale.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record TikTokInventoryIngressRequest(
        @NotBlank String receiptId,
        @NotBlank String sku,
        @NotNull @Min(0) Integer availableQty,
        @NotNull @Min(0) Integer reservedQty,
        @NotNull @Min(0) Integer soldQty,
        Instant observedAt
) {
}
