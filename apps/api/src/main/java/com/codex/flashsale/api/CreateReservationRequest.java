package com.codex.flashsale.api;

import com.codex.flashsale.common.domain.SalesChannel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(
        @NotBlank String sku,
        @NotNull SalesChannel channel,
        @Min(1) int quantity
) {
}

