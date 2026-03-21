package com.codex.flashsale.ai;

import java.util.List;

public record OpsCopilotModelFinding(
        String severity,
        String title,
        String detail,
        List<String> sourceIds
) {
}
