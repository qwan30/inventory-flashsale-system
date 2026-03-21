package com.codex.flashsale.api;

import java.util.List;

public record OpsCopilotCapabilitiesResponse(
        boolean enabled,
        boolean advisoryOnly,
        String provider,
        String model,
        List<String> allowedScopes,
        String statusMessage
) {
}
