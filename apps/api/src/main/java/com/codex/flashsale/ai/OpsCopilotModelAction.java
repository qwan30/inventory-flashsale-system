package com.codex.flashsale.ai;

import java.util.List;

public record OpsCopilotModelAction(
        String label,
        String href,
        String rationale,
        List<String> sourceIds
) {
}
