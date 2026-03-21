package com.codex.flashsale.api;

public record OpsCopilotProviderMetadataResponse(
        String provider,
        String model,
        boolean advisoryOnly,
        String requestId
) {
}
