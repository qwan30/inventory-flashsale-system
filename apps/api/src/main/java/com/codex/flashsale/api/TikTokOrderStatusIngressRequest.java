package com.codex.flashsale.api;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record TikTokOrderStatusIngressRequest(
        @NotBlank String receiptId,
        @NotBlank String orderId,
        @NotBlank String status,
        Instant observedAt
) {
}
