package com.codex.flashsale.api;

import jakarta.validation.constraints.NotBlank;

public record ResolveReconciliationDriftRequest(
        @NotBlank String resolutionNote
) {
}
