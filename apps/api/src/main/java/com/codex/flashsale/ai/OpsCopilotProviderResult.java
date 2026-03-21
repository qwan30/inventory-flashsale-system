package com.codex.flashsale.ai;

public record OpsCopilotProviderResult(
        String requestId,
        OpsCopilotModelResponse response
) {
}
