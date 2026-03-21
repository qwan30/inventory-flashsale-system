package com.codex.flashsale.api;

import java.util.List;

public record OpsCopilotFindingResponse(
        String severity,
        String title,
        String detail,
        List<String> sourceIds
) {
}
