package com.codex.flashsale.api;

import java.util.List;

public record OpsCopilotActionResponse(
        String label,
        String href,
        String rationale,
        List<String> sourceIds
) {
}
