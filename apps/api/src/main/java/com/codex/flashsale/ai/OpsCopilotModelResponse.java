package com.codex.flashsale.ai;

import java.util.List;

public record OpsCopilotModelResponse(
        String summary,
        List<OpsCopilotModelFinding> prioritizedFindings,
        List<OpsCopilotModelAction> recommendedActions
) {
}
