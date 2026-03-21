package com.codex.flashsale.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OpsCopilotAnalyzeRequest(
        @NotBlank String scope,
        @Size(max = 500) String focusQuestion,
        @Size(max = 128) String benchmarkRunId,
        @Size(max = 64) String channel,
        @Size(max = 128) String campaignId
) {
}
